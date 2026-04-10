package com.purehome.uicore.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.purehome.uicore.util.ContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_PAGES = "pages";
    public static final String CACHE_PAGE_VERSIONS = "pageVersions";
    public static final String CACHE_PUBLISHED_PAGES = "publishedPages";
    public static final String CACHE_LAYOUTS = "layouts";
    public static final String CACHE_METADATA = "metadata";
    public static final String CACHE_SESSIONS = "sessions";
    public static final String CACHE_RENDERED_LAYOUTS = "renderedLayouts";  // Added this

    // Cache metrics
    private final Map<String, AtomicLong> hitCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missCounts = new ConcurrentHashMap<>();

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        log.info("Initializing Caffeine Cache Manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_PAGES,
                CACHE_PAGE_VERSIONS,
                CACHE_PUBLISHED_PAGES,
                CACHE_LAYOUTS,
                CACHE_METADATA,
                CACHE_SESSIONS,
                CACHE_RENDERED_LAYOUTS  // Added this
        );

        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10_000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .weakKeys()
                .softValues()
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        log.debug("Cache evicted: {} - cause: {}", key, cause);
                    }
                });

        cacheManager.setCaffeine(caffeine);
        cacheManager.setAllowNullValues(false);

        return cacheManager;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing Redis Cache Manager");

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)
                )
                .prefixCacheNameWith("uicore:");

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CACHE_PAGES, defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(CACHE_PUBLISHED_PAGES, defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(CACHE_PAGE_VERSIONS, defaultConfig.entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration(CACHE_LAYOUTS, defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_RENDERED_LAYOUTS, defaultConfig.entryTtl(Duration.ofMinutes(5)))  // Added this
                .build();
    }

    @Bean
    public KeyGenerator pageKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName())
                    .append("_")
                    .append(method.getName());

            // Add tenant context if available
            ContextHolder.getTenantId().ifPresent(tenant ->
                    key.append("_tenant_").append(tenant));

            // Add user context if available
            ContextHolder.getUserId().ifPresent(user ->
                    key.append("_user_").append(user));

            for (Object param : params) {
                if (param != null) {
                    key.append("_").append(param.hashCode());
                }
            }

            return key.toString();
        };
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Cache GET error - Cache: {}, Key: {}, Error: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Cache PUT error - Cache: {}, Key: {}, Error: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Cache EVICT error - Cache: {}, Key: {}, Error: {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Cache CLEAR error - Cache: {}, Error: {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }

    public void recordHit(String cacheName) {
        hitCounts.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordMiss(String cacheName) {
        missCounts.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
    }

    public double getHitRate(String cacheName) {
        long hits = hitCounts.getOrDefault(cacheName, new AtomicLong()).get();
        long misses = missCounts.getOrDefault(cacheName, new AtomicLong()).get();
        long total = hits + misses;
        return total == 0 ? 0 : (double) hits / total;
    }

    @PreDestroy
    public void destroy() {
        log.info("Cache metrics summary:");
        for (String cache : List.of(CACHE_PAGES, CACHE_PAGE_VERSIONS, CACHE_PUBLISHED_PAGES, CACHE_RENDERED_LAYOUTS)) {
            log.info("  {} - Hit Rate: {}%", cache, String.format("%.2f", getHitRate(cache) * 100));
        }
    }
}