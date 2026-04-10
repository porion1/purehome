package com.purehome.uicore.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.time.Instant;
import java.util.*;

/**
 * FAANG-GRADE DEVELOPMENT AUTHENTICATION CONTROLLER
 *
 * ============================================================================
 * INNOVATION: Development Auth with Production Security Parity
 * ============================================================================
 * - Provides JWT token generation for development/testing
 * - Only active in 'dev' profile - automatically disabled in production
 * - Uses same JWT signing mechanism as production
 * - Supports role-based token generation for testing RBAC
 * - Includes token introspection for debugging
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@Profile("dev")  // Only active in development profile
@Tag(name = "Development Auth", description = "⚠️ DEVELOPMENT ONLY - JWT token generation for testing")
public class DevAuthController {

    @Value("${jwt.secret:default-secret-key-for-development-only-do-not-use-in-production}")
    private String jwtSecret;

    // Pre-defined test users for different roles
    private static final Map<String, TestUser> TEST_USERS = Map.of(
            "admin", new TestUser("admin-001", "admin@purehome.com", "ADMIN", List.of("ADMIN", "EDITOR", "VIEWER")),
            "editor", new TestUser("editor-001", "editor@purehome.com", "EDITOR", List.of("EDITOR", "VIEWER")),
            "viewer", new TestUser("viewer-001", "viewer@purehome.com", "VIEWER", List.of("VIEWER")),
            "premium", new TestUser("premium-001", "premium@purehome.com", "PREMIUM", List.of("VIEWER", "PREMIUM"))
    );

    /**
     * Generate JWT token for development
     */
    @PostMapping("/token")
    @Operation(summary = "Generate JWT Token", description = "⚠️ DEVELOPMENT ONLY - Generates JWT token for testing")
    public ResponseEntity<DevTokenResponse> generateToken(
            @RequestParam(defaultValue = "admin") String role,
            @RequestParam(required = false) Integer expiryHours) {

        TestUser user = TEST_USERS.get(role.toLowerCase());
        if (user == null) {
            return ResponseEntity.badRequest().body(DevTokenResponse.error(
                    "Invalid role. Available: " + String.join(", ", TEST_USERS.keySet())
            ));
        }

        int expiry = expiryHours != null ? expiryHours : 24;
        String token = generateJwtToken(user, expiry);

        log.info("Generated {} token for development - User: {}, Expires: {} hours",
                role, user.getUserId(), expiry);

        return ResponseEntity.ok(DevTokenResponse.success(
                token,
                user,
                Instant.now().plusSeconds(expiry * 3600L)
        ));
    }

    /**
     * Get available test users
     */
    @GetMapping("/users")
    @Operation(summary = "List test users", description = "Lists available test users with their roles")
    public ResponseEntity<List<TestUserInfo>> getTestUsers() {
        List<TestUserInfo> users = TEST_USERS.entrySet().stream()
                .map(e -> new TestUserInfo(
                        e.getKey(),
                        e.getValue().getUserId(),
                        e.getValue().getEmail(),
                        e.getValue().getPrimaryRole(),
                        e.getValue().getRoles()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    /**
     * Introspect token (verify token validity)
     */
    @PostMapping("/introspect")
    @Operation(summary = "Introspect token", description = "Verify JWT token validity")
    public ResponseEntity<Map<String, Object>> introspectToken(
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("active", false);
            response.put("message", "Invalid token format");
            return ResponseEntity.ok(response);
        }

        String token = authHeader.substring(7);

        try {
            Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

            io.jsonwebtoken.Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            response.put("active", true);
            response.put("subject", claims.getSubject());
            response.put("roles", claims.get("roles"));
            response.put("userId", claims.get("user_id"));
            response.put("email", claims.get("email"));
            response.put("issuedAt", claims.getIssuedAt());
            response.put("expiration", claims.getExpiration());

        } catch (Exception e) {
            response.put("active", false);
            response.put("message", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Generate JWT token using JJWT library
     */
    private String generateJwtToken(TestUser user, int expiryHours) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + (expiryHours * 3600L * 1000L));

        return Jwts.builder()
                .setSubject(user.getUserId())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("user_id", user.getUserId())
                .claim("email", user.getEmail())
                .claim("roles", user.getRoles())
                .claim("primary_role", user.getPrimaryRole())
                .claim("workspace_id", "default")
                .claim("tenant_id", "purehome")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // =========================================================================
    // Inner Classes
    // =========================================================================

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TestUser {
        private String userId;
        private String email;
        private String primaryRole;
        private List<String> roles;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TestUserInfo {
        private String roleKey;
        private String userId;
        private String email;
        private String primaryRole;
        private List<String> roles;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class DevTokenResponse {
        private boolean success;
        private String token;
        private String type;
        private TestUserInfo user;
        private Instant expiresAt;
        private String error;

        public static DevTokenResponse success(String token, TestUser user, Instant expiresAt) {
            return new DevTokenResponse(
                    true,
                    "Bearer " + token,
                    "Bearer",
                    new TestUserInfo(
                            user.getPrimaryRole().toLowerCase(),
                            user.getUserId(),
                            user.getEmail(),
                            user.getPrimaryRole(),
                            user.getRoles()
                    ),
                    expiresAt,
                    null
            );
        }

        public static DevTokenResponse error(String message) {
            return new DevTokenResponse(false, null, null, null, null, message);
        }
    }
}