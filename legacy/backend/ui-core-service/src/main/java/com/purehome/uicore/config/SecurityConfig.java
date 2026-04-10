package com.purehome.uicore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${security.rate-limit.default:100}")
    private int defaultRateLimit;

    @Value("${security.rate-limit.premium:500}")
    private int premiumRateLimit;

    @Value("${security.rate-limit.admin:1000}")
    private int adminRateLimit;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health/**",
            "/actuator/info",
            "/api/public/**",
            "/api/dev/**",
            "/api/v1/pages/published/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // =========================================================================
    // Rate Limiter Implementation
    // =========================================================================

    private static final class TokenBucket {
        private final int capacity;
        private final int refillRate;
        private int tokens;
        private long lastRefillTime;

        public TokenBucket(int capacity, int refillRatePerMinute) {
            this.capacity = capacity;
            this.refillRate = refillRatePerMinute;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            int tokensToAdd = (int) (timePassed * refillRate / 60000);
            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }

    private static final class RateLimiter {
        private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
        private final int defaultLimit;
        private final int premiumLimit;
        private final int adminLimit;

        public RateLimiter(int defaultLimit, int premiumLimit, int adminLimit) {
            this.defaultLimit = defaultLimit;
            this.premiumLimit = premiumLimit;
            this.adminLimit = adminLimit;
        }

        public boolean allowRequest(String key, String tier) {
            int limit = switch (tier) {
                case "ADMIN" -> adminLimit;
                case "PREMIUM" -> premiumLimit;
                default -> defaultLimit;
            };

            TokenBucket bucket = buckets.computeIfAbsent(key,
                    k -> new TokenBucket(limit, limit));

            return bucket.tryConsume();
        }
    }

    // =========================================================================
    // Rate Limiting Filter
    // =========================================================================

    private class RateLimitingFilter extends OncePerRequestFilter {

        private final RateLimiter rateLimiter;

        public RateLimitingFilter(RateLimiter rateLimiter) {
            this.rateLimiter = rateLimiter;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String key = getRateLimitKey(request);
            String tier = extractUserTier(request);

            if (!rateLimiter.allowRequest(key, tier)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                Map<String, Object> error = Map.of(
                        "error", "Rate limit exceeded",
                        "retryAfter", "60",
                        "timestamp", Instant.now().toString()
                );

                new ObjectMapper().writeValue(response.getWriter(), error);
                return;
            }

            filterChain.doFilter(request, response);
        }

        private String getRateLimitKey(HttpServletRequest request) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return "user:" + authHeader.substring(7).hashCode();
            }
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null) ip = request.getRemoteAddr();
            return "ip:" + ip;
        }

        private String extractUserTier(HttpServletRequest request) {
            return "DEFAULT";
        }
    }

    // =========================================================================
    // User ID Filter - Extracts userId from JWT and adds to request attributes
    // =========================================================================

    private class UserIdFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Jwt) {
                    Jwt jwt = (Jwt) principal;
                    String userId = jwt.getClaimAsString("user_id");
                    if (userId != null) {
                        request.setAttribute("userId", userId);
                        log.debug("Set userId attribute: {}", userId);
                    } else {
                        // Fallback to subject if user_id not present
                        String subject = jwt.getSubject();
                        if (subject != null) {
                            request.setAttribute("userId", subject);
                            log.debug("Set userId attribute from subject: {}", subject);
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        }
    }

    // =========================================================================
    // Security Filter Chain
    // =========================================================================

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Create rate limiter with configured limits
        RateLimiter rateLimiter = new RateLimiter(defaultRateLimit, premiumRateLimit, adminRateLimit);

        http
                .cors().and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(new RateLimitingFilter(rateLimiter), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new UserIdFilter(), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/pages/versions/**").hasAnyRole("ADMIN", "EDITOR")
                        .requestMatchers("/api/v1/pages/**").hasAnyRole("ADMIN", "EDITOR", "VIEWER")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            Map<String, Object> error = new HashMap<>();
                            error.put("error", "Unauthorized");
                            error.put("message", authException.getMessage());
                            error.put("timestamp", Instant.now().toString());
                            new ObjectMapper().writeValue(response.getWriter(), error);
                        })
                );

        return http.build();
    }

    // =========================================================================
    // CORS Configuration
    // =========================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://*.purehome.com",
                "http://localhost:3000",
                "http://localhost:3001"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Request-ID", "X-API-Key", "X-Tenant-ID"
        ));
        configuration.setExposedHeaders(List.of("X-Total-Count", "X-Rate-Limit"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // =========================================================================
    // JWT Configuration
    // =========================================================================

    @Bean
    public JwtDecoder jwtDecoder() {
        String secretKey = System.getenv("JWT_SECRET");

        if (secretKey == null || secretKey.isEmpty()) {
            log.warn("JWT_SECRET environment variable not set. Using default secret for development. DO NOT USE IN PRODUCTION!");
            secretKey = "default-secret-key-for-development-only-do-not-use-in-production";
        }

        return NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secretKey.getBytes(), "HmacSHA256")
        ).build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        converter.setPrincipalClaimName("sub");

        return converter;
    }
}