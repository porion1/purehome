package com.purehome.uicore.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.*;
import com.purehome.uicore.util.ContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
// REMOVED: @EnableMongoAuditing - Moved to main application class
@EnableMongoRepositories(
        basePackages = "com.purehome.uicore.repository"
)
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    // Configuration properties
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.connection-pool.max-size:100}")
    private int maxPoolSize;

    @Value("${spring.data.mongodb.connection-pool.min-size:10}")
    private int minPoolSize;

    @Value("${spring.data.mongodb.connection-pool.max-wait-time:5000}")
    private int maxWaitTimeMs;

    private MongoClient mongoClient;

    // Connection metrics
    private final AtomicLong currentConnections = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final Map<String, CommandStats> commandStats = new ConcurrentHashMap<>();

    private static class CommandStats {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong totalTimeMicros = new AtomicLong();

        public void record(long timeMicros) {
            count.incrementAndGet();
            totalTimeMicros.addAndGet(timeMicros);
        }

        public double getAvgTimeMicros() {
            long c = count.get();
            return c == 0 ? 0 : (double) totalTimeMicros.get() / c;
        }
    }

    @Bean
    @Primary
    public MongoClient mongoClient() {
        log.info("Initializing MongoDB Client");

        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(maxPoolSize)
                        .minSize(minPoolSize)
                        .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                        .addConnectionPoolListener(new ConnectionPoolListener() {
                            @Override
                            public void connectionAdded(ConnectionAddedEvent event) {
                                currentConnections.incrementAndGet();
                            }

                            @Override
                            public void connectionRemoved(ConnectionRemovedEvent event) {
                                currentConnections.decrementAndGet();
                            }

                            @Override
                            public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
                                totalRequests.incrementAndGet();
                            }
                        })
                )
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(2000, TimeUnit.MILLISECONDS)
                        .readTimeout(5000, TimeUnit.MILLISECONDS)
                )
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(5000, TimeUnit.MILLISECONDS)
                )
                .readConcern(ReadConcern.MAJORITY)
                .writeConcern(WriteConcern.MAJORITY.withJournal(true))
                .readPreference(ReadPreference.primaryPreferred())
                .retryWrites(true)
                .retryReads(true)
                .applicationName("ui-core-service")
                .addCommandListener(new CommandListener() {
                    @Override
                    public void commandStarted(CommandStartedEvent event) {
                        ContextHolder.setCommandName(event.getCommandName());
                        ContextHolder.setCommandStartTime(System.nanoTime());
                    }

                    @Override
                    public void commandSucceeded(CommandSucceededEvent event) {
                        long startTime = ContextHolder.getCommandStartTime().orElse(System.nanoTime());
                        long durationMicros = (System.nanoTime() - startTime) / 1000;

                        commandStats.computeIfAbsent(event.getCommandName(), k -> new CommandStats())
                                .record(durationMicros);

                        if (durationMicros > 100_000) {
                            log.warn("Slow query: {} took {}ms", event.getCommandName(), durationMicros / 1000);
                        }
                    }

                    @Override
                    public void commandFailed(CommandFailedEvent event) {
                        log.error("Command failed: {} - {}", event.getCommandName(), event.getThrowable().getMessage());
                    }
                })
                .build();

        this.mongoClient = MongoClients.create(settings);

        verifyConnection();

        log.info("MongoDB Client initialized - Pool: {}/{}", minPoolSize, maxPoolSize);

        return this.mongoClient;
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        MongoTemplate template = new MongoTemplate(mongoClient, databaseName);
        template.setWriteConcern(WriteConcern.MAJORITY.withJournal(true));
        return template;
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            String username = Optional.ofNullable(
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                    )
                    .map(auth -> auth.getName())
                    .filter(name -> !"anonymousUser".equals(name))
                    .orElse("SYSTEM");

            Optional<String> traceId = ContextHolder.getTraceId();
            if (traceId.isPresent()) {
                username = username + "|trace:" + traceId.get();
            }

            return Optional.of(username);
        };
    }

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(Instant.now().atZone(ZoneOffset.UTC));
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory databaseFactory,
            MongoCustomConversions customConversions,
            MongoMappingContext mappingContext) {

        MappingMongoConverter converter = new MappingMongoConverter(
                new org.springframework.data.mongodb.core.convert.DefaultDbRefResolver(databaseFactory),
                mappingContext
        );

        converter.setCustomConversions(customConversions);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setMapKeyDotReplacement("_");

        return converter;
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(new ArrayList<>());
    }

    private void verifyConnection() {
        try {
            MongoTemplate template = new MongoTemplate(mongoClient, databaseName);
            template.executeCommand("{ ping: 1 }");
            log.info("MongoDB connection verified");
        } catch (Exception e) {
            log.error("MongoDB connection failed: {}", e.getMessage());
            throw new RuntimeException("MongoDB connection failed", e);
        }
    }

    @PostConstruct
    public void init() {
        log.info("MongoDB Configuration initialized");
    }

    @PreDestroy
    public void destroy() {
        if (mongoClient != null) {
            log.info("Closing MongoDB connections...");
            mongoClient.close();
        }
    }
}