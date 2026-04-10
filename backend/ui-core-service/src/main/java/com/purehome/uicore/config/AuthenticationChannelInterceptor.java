package com.purehome.uicore.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

/**
 * WebSocket Authentication Interceptor
 * Validates JWT tokens for WebSocket connections
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@Component
public class AuthenticationChannelInterceptor implements ChannelInterceptor {

    private JwtDecoder jwtDecoder;

    public AuthenticationChannelInterceptor() {
        String secretKey = System.getenv("JWT_SECRET");
        if (secretKey == null || secretKey.isEmpty()) {
            secretKey = "default-secret-key-for-development-only-change-in-production";
        }
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(secretKey.getBytes(), "HmacSHA256")
        ).build();
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.CONNECT) {
            // Extract token from Authorization header
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.get(0).replace("Bearer ", "");
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    // Create authentication object
                    Authentication auth = new JwtAuthenticationToken(jwt);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    accessor.setUser(auth);
                    log.debug("WebSocket connection authenticated for user: {}", jwt.getSubject());
                } catch (Exception e) {
                    log.warn("WebSocket authentication failed: {}", e.getMessage());
                    throw new SecurityException("Invalid JWT token");
                }
            } else {
                log.warn("WebSocket connection attempt without authentication");
                throw new SecurityException("Authentication required");
            }
        } else if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            // Verify authentication exists
            if (accessor.getUser() == null) {
                log.warn("Unauthorized WebSocket {} attempt", command);
                throw new SecurityException("Authentication required");
            }
        }

        return message;
    }

    /**
     * Simple JWT Authentication Token
     */
    private static class JwtAuthenticationToken implements Authentication {
        private final Jwt jwt;

        public JwtAuthenticationToken(Jwt jwt) {
            this.jwt = jwt;
        }

        @Override
        public String getName() {
            return jwt.getSubject();
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return jwt;
        }

        @Override
        public Object getPrincipal() {
            return jwt;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            throw new UnsupportedOperationException("Cannot change authentication");
        }
    }
}