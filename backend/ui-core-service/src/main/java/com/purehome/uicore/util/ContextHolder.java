package com.purehome.uicore.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ThreadLocal context holder for request-scoped data
 * Used for propagating context across layers without passing parameters
 */
public final class ContextHolder {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private ContextHolder() {}

    public static void set(String key, Object value) {
        CONTEXT.get().put(key, value);
    }

    public static <T> Optional<T> get(String key, Class<T> type) {
        Object value = CONTEXT.get().get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public static void remove(String key) {
        CONTEXT.get().remove(key);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    // Convenience methods for common context values
    public static void setUserId(String userId) {
        set("userId", userId);
    }

    public static Optional<String> getUserId() {
        return get("userId", String.class);
    }

    public static void setUserTier(int tier) {
        set("userTier", tier);
    }

    public static Optional<Integer> getUserTier() {
        return get("userTier", Integer.class);
    }

    public static void setTenantId(String tenantId) {
        set("tenantId", tenantId);
    }

    public static Optional<String> getTenantId() {
        return get("tenantId", String.class);
    }

    public static void setCommandStartTime(long startTime) {
        set("commandStartTime", startTime);
    }

    public static Optional<Long> getCommandStartTime() {
        return get("commandStartTime", Long.class);
    }

    public static void setCommandName(String commandName) {
        set("commandName", commandName);
    }

    public static Optional<String> getCommandName() {
        return get("commandName", String.class);
    }

    public static void setOperation(String operation) {
        set("operation", operation);
    }

    public static Optional<String> getOperation() {
        return get("operation", String.class);
    }

    public static void setTraceId(String traceId) {
        set("traceId", traceId);
    }

    public static Optional<String> getTraceId() {
        return get("traceId", String.class);
    }
}