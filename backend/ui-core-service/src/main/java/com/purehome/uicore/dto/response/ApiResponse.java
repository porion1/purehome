package com.purehome.uicore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * FAANG-GRADE API RESPONSE WRAPPER
 *
 * ============================================================================
 * INNOVATION ALGORITHM 1: Standardized Response Structure
 * ============================================================================
 * - Provides consistent API response format across all endpoints
 * - Includes metadata for debugging and monitoring (timestamp, correlation ID)
 * - Supports generic typing for type-safe responses
 * - Enables easy client-side parsing and error handling
 *
 * ============================================================================
 * INNOVATION ALGORITHM 2: Intelligent Response Factory
 * ============================================================================
 * - Provides factory methods for common HTTP status codes
 * - Automatically sets appropriate messages based on status
 * - Supports both success and error responses with consistent structure
 * - Enables fluent API for response building
 *
 * @author PureHome Engineering
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    // =========================================================================
    // Core Fields
    // =========================================================================

    /**
     * Indicates whether the operation was successful
     */
    private boolean success;

    /**
     * HTTP status code (200, 201, 400, 404, 500, etc.)
     */
    private int status;

    /**
     * Human-readable message about the operation result
     */
    private String message;

    /**
     * The actual response data (generic type)
     */
    private T data;

    /**
     * Error details (only present for error responses)
     */
    private String error;

    /**
     * Timestamp when the response was generated
     */
    private Instant timestamp;

    /**
     * Correlation ID for distributed tracing and debugging
     */
    private String correlationId;

    /**
     * Request path that generated this response
     */
    private String path;

    // =========================================================================
    // Pagination Metadata (Optional)
    // =========================================================================

    /**
     * Pagination metadata for list responses
     */
    private PaginationMeta pagination;

    /**
     * Pagination metadata inner class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationMeta {
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
        private String nextCursor;
        private String previousCursor;
    }

    // =========================================================================
    // Factory Methods - Success Responses
    // =========================================================================

    /**
     * Create a success response with data (HTTP 200)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with data and custom message (HTTP 200)
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a success response with pagination metadata (HTTP 200)
     */
    public static <T> ApiResponse<T> success(T data, PaginationMeta pagination) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .data(data)
                .pagination(pagination)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a created response (HTTP 201)
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(201)
                .message("Created successfully")
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a created response with custom message (HTTP 201)
     */
    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(201)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a no content response (HTTP 204)
     */
    public static ApiResponse<Void> noContent() {
        return ApiResponse.<Void>builder()
                .success(true)
                .status(204)
                .message("No content")
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create a no content response with custom message (HTTP 204)
     */
    public static ApiResponse<Void> noContent(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .status(204)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an accepted response (HTTP 202)
     */
    public static <T> ApiResponse<T> accepted(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(202)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an accepted response (HTTP 202) without data
     */
    public static ApiResponse<Void> accepted(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .status(202)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    // =========================================================================
    // Factory Methods - Error Responses
    // =========================================================================

    /**
     * Create a bad request error response (HTTP 400)
     */
    public static ApiResponse<Void> badRequest(String message) {
        return error(400, "Bad Request", message);
    }

    /**
     * Create a bad request error response with details (HTTP 400)
     */
    public static ApiResponse<Void> badRequest(String message, String error) {
        return error(400, error, message);
    }

    /**
     * Create an unauthorized error response (HTTP 401)
     */
    public static ApiResponse<Void> unauthorized(String message) {
        return error(401, "Unauthorized", message);
    }

    /**
     * Create a forbidden error response (HTTP 403)
     */
    public static ApiResponse<Void> forbidden(String message) {
        return error(403, "Forbidden", message);
    }

    /**
     * Create a not found error response (HTTP 404)
     */
    public static ApiResponse<Void> notFound(String message) {
        return error(404, "Not Found", message);
    }

    /**
     * Create a conflict error response (HTTP 409)
     */
    public static ApiResponse<Void> conflict(String message) {
        return error(409, "Conflict", message);
    }

    /**
     * Create an unprocessable entity error response (HTTP 422)
     */
    public static ApiResponse<Void> unprocessableEntity(String message) {
        return error(422, "Unprocessable Entity", message);
    }

    /**
     * Create an internal server error response (HTTP 500)
     */
    public static ApiResponse<Void> internalServerError(String message) {
        return error(500, "Internal Server Error", message);
    }

    /**
     * Create a service unavailable error response (HTTP 503)
     */
    public static ApiResponse<Void> serviceUnavailable(String message) {
        return error(503, "Service Unavailable", message);
    }

    /**
     * Generic error response factory
     */
    public static ApiResponse<Void> error(int status, String error, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Generic error response with correlation ID
     */
    public static ApiResponse<Void> error(int status, String error, String message, String correlationId) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    // =========================================================================
    // Factory Methods with Path and Correlation ID
    // =========================================================================

    /**
     * Create a success response with path and correlation ID
     */
    public static <T> ApiResponse<T> success(T data, String path, String correlationId) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(200)
                .message("Success")
                .data(data)
                .path(path)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Create an error response with path and correlation ID
     */
    public static ApiResponse<Void> error(int status, String error, String message, String path, String correlationId) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Check if the response is a success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the response has data
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Check if the response has pagination metadata
     */
    public boolean hasPagination() {
        return pagination != null;
    }

    /**
     * Convert to simplified map for logging
     */
    public java.util.Map<String, Object> toLogMap() {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("success", success);
        map.put("status", status);
        map.put("message", message);
        map.put("timestamp", timestamp);
        map.put("correlationId", correlationId);
        if (pagination != null) {
            map.put("hasMore", pagination.isHasNext());
            map.put("totalElements", pagination.getTotalElements());
        }
        return map;
    }
}