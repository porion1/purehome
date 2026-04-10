package com.purehome.uicore;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.TimeZone;

@SpringBootApplication
@EnableMongoAuditing
@EnableCaching
@EnableAsync
@EnableRetry
@EnableScheduling
@EnableConfigurationProperties
@EnableMongoRepositories(basePackages = {
		"com.purehome.uicore.repository"
})
public class UiCoreServiceApplication {

	public static void main(String[] args) {
		// Set default timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

		// Configure system properties for optimal performance
		System.setProperty("java.awt.headless", "true");
		System.setProperty("spring.application.name", "ui-core-service");

		// Start the application with performance monitoring
		long startTime = System.currentTimeMillis();
		SpringApplication.run(UiCoreServiceApplication.class, args);

		long startupTime = System.currentTimeMillis() - startTime;
		System.out.printf("""
            %n=========================================================
            ✅ UI Core Service started successfully!
            🚀 Startup time: %d ms
            🕐 System time: %s
            =========================================================%n""",
				startupTime,
				Instant.now().toString()
		);
	}

	@Bean
	public MeterRegistry meterRegistry() {
		return new SimpleMeterRegistry();
	}

	@Bean
	public ApplicationStartupLogger applicationStartupLogger(Environment environment) {
		return new ApplicationStartupLogger(environment);
	}

	// Inner class to handle non-static environment access
	public static class ApplicationStartupLogger {

		private final Environment environment;

		public ApplicationStartupLogger(Environment environment) {
			this.environment = environment;
		}

		@EventListener(ApplicationReadyEvent.class)
		public void logApplicationStartup() {
			try {
				String protocol = environment.getProperty("server.ssl.key-store") != null ? "https" : "http";
				String host = InetAddress.getLocalHost().getHostAddress();
				String port = environment.getProperty("server.port", "8081");

				System.out.printf("""
                    %n----------------------------------------------------------
                    \tApplication '%s' is running! Access URLs:
                    \tLocal: \t\t%s://localhost:%s
                    \tExternal: \t%s://%s:%s
                    \tProfile(s): \t%s
                    \tHealth: \t%s://localhost:%s/actuator/health
                    ----------------------------------------------------------%n""",
						environment.getProperty("spring.application.name", "ui-core-service"),
						protocol,
						port,
						protocol,
						host,
						port,
						String.join(", ", environment.getActiveProfiles()),
						protocol,
						port
				);
			} catch (Exception e) {
				// Log but don't fail startup
				System.err.println("Could not log application startup: " + e.getMessage());
			}
		}
	}
}