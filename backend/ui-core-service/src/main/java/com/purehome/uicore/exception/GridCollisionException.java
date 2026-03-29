package com.purehome.uicore.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * FAANG-ULTRA GRID COLLISION EXCEPTION
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Quantum Collision Resolution Exception (QCRE)
 * - Provides comprehensive collision detection metadata
 * - Includes multiple resolution strategies with confidence scoring
 * - Supports nested collision chains with dependency tracking
 * - Provides visual collision map for debugging
 * - Includes auto-resolution suggestions with preview
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Getter
public class GridCollisionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // =========================================================================
    // COLLISION IDENTIFIERS
    // =========================================================================

    private final String collisionId;
    private final String componentId;
    private final String pageId;
    private final String sectionId;
    private final String workspaceId;

    // =========================================================================
    // COLLISION GEOMETRY
    // =========================================================================

    private final CollisionBounds attemptedBounds;
    private final List<CollisionBounds> collidingBounds;
    private final List<String> collidingComponentIds;
    private final Map<String, CollisionBounds> collisionMap;

    // =========================================================================
    // COLLISION METRICS
    // =========================================================================

    private final int collisionCount;
    private final double overlapArea;
    private final double overlapPercentage;
    private final int gridColumns;
    private final int gridRows;
    private final double severityScore;

    // =========================================================================
    // RESOLUTION STRATEGIES
    // =========================================================================

    private final List<ResolutionStrategy> resolutionStrategies;
    private final Map<String, CollisionBounds> resolvedPositions;
    private final String bestResolutionStrategy;
    private final double bestResolutionConfidence;

    // =========================================================================
    // COLLISION CHAIN
    // =========================================================================

    private final List<CollisionChain> collisionChain;
    private final boolean hasCircularCollision;
    private final List<String> circularCollisionPath;

    // =========================================================================
    // VISUALIZATION DATA
    // =========================================================================

    private final int[][] collisionHeatmap;
    private final Map<String, int[]> componentPositions;

    // =========================================================================
    // METADATA
    // =========================================================================

    private final Instant detectedAt;
    private final String correlationId;
    private final boolean autoResolvable;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Getter
    public static class CollisionBounds {
        private final String componentId;
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final int zIndex;

        public CollisionBounds(String componentId, int x, int y, int width, int height) {
            this(componentId, x, y, width, height, 0);
        }

        public CollisionBounds(String componentId, int x, int y, int width, int height, int zIndex) {
            this.componentId = componentId;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.zIndex = zIndex;
        }

        public boolean intersects(CollisionBounds other) {
            return !(other.x >= x + width ||
                    other.x + other.width <= x ||
                    other.y >= y + height ||
                    other.y + other.height <= y);
        }

        public int getOverlapArea(CollisionBounds other) {
            if (!intersects(other)) return 0;

            int overlapX = Math.min(x + width, other.x + other.width) - Math.max(x, other.x);
            int overlapY = Math.min(y + height, other.y + other.height) - Math.max(y, other.y);
            return overlapX * overlapY;
        }

        public String getBoundsString() {
            return String.format("[%d,%d %dx%d]", x, y, width, height);
        }

        @Override
        public String toString() {
            return String.format("%s: %s (z=%d)", componentId, getBoundsString(), zIndex);
        }
    }

    @Getter
    public static class ResolutionStrategy {
        private final String id;
        private final String type;
        private final String description;
        private final CollisionBounds targetBounds;
        private final double confidence;
        private final boolean autoApplicable;
        private final int affectedComponents;
        private final double movementDistance;

        public ResolutionStrategy(String id, String type, String description,
                                  CollisionBounds targetBounds, double confidence,
                                  boolean autoApplicable, int affectedComponents,
                                  double movementDistance) {
            this.id = id;
            this.type = type;
            this.description = description;
            this.targetBounds = targetBounds;
            this.confidence = confidence;
            this.autoApplicable = autoApplicable;
            this.affectedComponents = affectedComponents;
            this.movementDistance = movementDistance;
        }

        public String getSummary() {
            return String.format("%s (%.1f%%): %s -> %s",
                    type, confidence * 100, description, targetBounds.getBoundsString());
        }
    }

    @Getter
    public static class CollisionChain {
        private final String rootComponentId;
        private final List<String> chain;
        private final List<CollisionBounds> bounds;
        private final int depth;
        private final double totalOverlap;

        public CollisionChain(String rootComponentId, List<String> chain,
                              List<CollisionBounds> bounds, int depth, double totalOverlap) {
            this.rootComponentId = rootComponentId;
            this.chain = chain;
            this.bounds = bounds;
            this.depth = depth;
            this.totalOverlap = totalOverlap;
        }

        public String getChainString() {
            return String.join(" -> ", chain);
        }
    }

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    public GridCollisionException(String componentId, int x, int y, int width, int height) {
        super(String.format("Grid collision detected for component %s at position [%d,%d] size %dx%d",
                componentId, x, y, width, height));
        this.collisionId = UUID.randomUUID().toString();
        this.componentId = componentId;
        this.pageId = null;
        this.sectionId = null;
        this.workspaceId = null;
        this.attemptedBounds = new CollisionBounds(componentId, x, y, width, height);
        this.collidingBounds = new ArrayList<>();
        this.collidingComponentIds = new ArrayList<>();
        this.collisionMap = new HashMap<>();
        this.collisionCount = 0;
        this.overlapArea = 0;
        this.overlapPercentage = 0;
        this.gridColumns = 12;
        this.gridRows = 24;
        this.severityScore = 0;
        this.resolutionStrategies = new ArrayList<>();
        this.resolvedPositions = new HashMap<>();
        this.bestResolutionStrategy = null;
        this.bestResolutionConfidence = 0;
        this.collisionChain = new ArrayList<>();
        this.hasCircularCollision = false;
        this.circularCollisionPath = new ArrayList<>();
        this.collisionHeatmap = new int[0][0];
        this.componentPositions = new HashMap<>();
        this.detectedAt = Instant.now();
        this.correlationId = null;
        this.autoResolvable = false;
    }

    // =========================================================================
    // BUILDER PATTERN
    // =========================================================================

    public static class Builder {
        private String componentId;
        private String pageId;
        private String sectionId;
        private String workspaceId;
        private CollisionBounds attemptedBounds;
        private List<CollisionBounds> collidingBounds = new ArrayList<>();
        private Map<String, CollisionBounds> collisionMap = new HashMap<>();
        private int gridColumns = 12;
        private int gridRows = 24;
        private List<ResolutionStrategy> resolutionStrategies = new ArrayList<>();
        private Map<String, CollisionBounds> resolvedPositions = new HashMap<>();
        private String bestResolutionStrategy;
        private double bestResolutionConfidence;
        private List<CollisionChain> collisionChain = new ArrayList<>();
        private boolean hasCircularCollision;
        private List<String> circularCollisionPath = new ArrayList<>();
        private int[][] collisionHeatmap;
        private Map<String, int[]> componentPositions = new HashMap<>();
        private String correlationId;

        public Builder componentId(String componentId) {
            this.componentId = componentId;
            return this;
        }

        public Builder pageId(String pageId) {
            this.pageId = pageId;
            return this;
        }

        public Builder sectionId(String sectionId) {
            this.sectionId = sectionId;
            return this;
        }

        public Builder workspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder attemptedBounds(CollisionBounds bounds) {
            this.attemptedBounds = bounds;
            return this;
        }

        public Builder attemptedBounds(String componentId, int x, int y, int width, int height) {
            this.attemptedBounds = new CollisionBounds(componentId, x, y, width, height);
            return this;
        }

        public Builder addCollidingBounds(CollisionBounds bounds) {
            this.collidingBounds.add(bounds);
            this.collisionMap.put(bounds.getComponentId(), bounds);
            return this;
        }

        public Builder addCollidingBounds(List<CollisionBounds> bounds) {
            this.collidingBounds.addAll(bounds);
            for (CollisionBounds b : bounds) {
                this.collisionMap.put(b.getComponentId(), b);
            }
            return this;
        }

        public Builder gridDimensions(int columns, int rows) {
            this.gridColumns = columns;
            this.gridRows = rows;
            return this;
        }

        public Builder addResolutionStrategy(ResolutionStrategy strategy) {
            this.resolutionStrategies.add(strategy);
            return this;
        }

        public Builder addResolutionStrategies(List<ResolutionStrategy> strategies) {
            this.resolutionStrategies.addAll(strategies);
            return this;
        }

        public Builder resolvedPosition(String componentId, CollisionBounds bounds) {
            this.resolvedPositions.put(componentId, bounds);
            return this;
        }

        public Builder bestResolution(String strategy, double confidence) {
            this.bestResolutionStrategy = strategy;
            this.bestResolutionConfidence = confidence;
            return this;
        }

        public Builder addCollisionChain(CollisionChain chain) {
            this.collisionChain.add(chain);
            return this;
        }

        public Builder circularCollision(List<String> path) {
            this.hasCircularCollision = true;
            this.circularCollisionPath = path;
            return this;
        }

        public Builder heatmap(int[][] heatmap) {
            this.collisionHeatmap = heatmap;
            return this;
        }

        public Builder componentPositions(Map<String, int[]> positions) {
            this.componentPositions = positions;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public GridCollisionException build() {
            return new GridCollisionException(this);
        }
    }

    private GridCollisionException(Builder builder) {
        super(buildMessage(builder));
        this.collisionId = UUID.randomUUID().toString();
        this.componentId = builder.componentId;
        this.pageId = builder.pageId;
        this.sectionId = builder.sectionId;
        this.workspaceId = builder.workspaceId;
        this.attemptedBounds = builder.attemptedBounds;
        this.collidingBounds = builder.collidingBounds;
        this.collidingComponentIds = builder.collidingBounds.stream()
                .map(CollisionBounds::getComponentId)
                .collect(Collectors.toList());
        this.collisionMap = builder.collisionMap;
        this.collisionCount = builder.collidingBounds.size();
        this.overlapArea = calculateTotalOverlap(builder.attemptedBounds, builder.collidingBounds);
        this.overlapPercentage = calculateOverlapPercentage(builder.attemptedBounds, this.overlapArea);
        this.gridColumns = builder.gridColumns;
        this.gridRows = builder.gridRows;
        this.severityScore = calculateSeverityScore(this.overlapPercentage, this.collisionCount);
        this.resolutionStrategies = builder.resolutionStrategies;
        this.resolvedPositions = builder.resolvedPositions;
        this.bestResolutionStrategy = builder.bestResolutionStrategy;
        this.bestResolutionConfidence = builder.bestResolutionConfidence;
        this.collisionChain = builder.collisionChain;
        this.hasCircularCollision = builder.hasCircularCollision;
        this.circularCollisionPath = builder.circularCollisionPath;
        this.collisionHeatmap = builder.collisionHeatmap != null ?
                builder.collisionHeatmap : generateHeatmap(builder);
        this.componentPositions = builder.componentPositions;
        this.detectedAt = Instant.now();
        this.correlationId = builder.correlationId;
        this.autoResolvable = !builder.resolutionStrategies.isEmpty() &&
                builder.resolutionStrategies.stream().anyMatch(ResolutionStrategy::isAutoApplicable);
    }

    public static Builder builder() {
        return new Builder();
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static String buildMessage(Builder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append("Grid collision detected");

        if (builder.componentId != null) {
            sb.append(" for component: ").append(builder.componentId);
        }

        if (builder.attemptedBounds != null) {
            sb.append(" at ").append(builder.attemptedBounds.getBoundsString());
        }

        if (!builder.collidingBounds.isEmpty()) {
            sb.append(" colliding with ").append(builder.collidingBounds.size());
            sb.append(" component(s)");
        }

        return sb.toString();
    }

    private static double calculateTotalOverlap(CollisionBounds bounds, List<CollisionBounds> colliding) {
        if (bounds == null || colliding == null || colliding.isEmpty()) return 0;

        double totalOverlap = 0;
        for (CollisionBounds other : colliding) {
            totalOverlap += bounds.getOverlapArea(other);
        }
        return totalOverlap;
    }

    private static double calculateOverlapPercentage(CollisionBounds bounds, double overlapArea) {
        if (bounds == null) return 0;
        double totalArea = bounds.getWidth() * bounds.getHeight();
        return totalArea > 0 ? (overlapArea / totalArea) * 100 : 0;
    }

    private static double calculateSeverityScore(double overlapPercentage, int collisionCount) {
        double overlapScore = Math.min(100, overlapPercentage);
        double countScore = Math.min(100, collisionCount * 20);
        return (overlapScore * 0.7) + (countScore * 0.3);
    }

    private static int[][] generateHeatmap(Builder builder) {
        if (builder.gridColumns <= 0 || builder.gridRows <= 0) {
            return new int[0][0];
        }

        int[][] heatmap = new int[builder.gridRows][builder.gridColumns];

        // Mark attempted bounds
        if (builder.attemptedBounds != null) {
            for (int y = builder.attemptedBounds.getY();
                 y < builder.attemptedBounds.getY() + builder.attemptedBounds.getHeight() && y < builder.gridRows; y++) {
                for (int x = builder.attemptedBounds.getX();
                     x < builder.attemptedBounds.getX() + builder.attemptedBounds.getWidth() && x < builder.gridColumns; x++) {
                    if (y >= 0 && x >= 0) {
                        heatmap[y][x] += 1;
                    }
                }
            }
        }

        // Mark colliding bounds
        for (CollisionBounds bounds : builder.collidingBounds) {
            for (int y = bounds.getY(); y < bounds.getY() + bounds.getHeight() && y < builder.gridRows; y++) {
                for (int x = bounds.getX(); x < bounds.getX() + bounds.getWidth() && x < builder.gridColumns; x++) {
                    if (y >= 0 && x >= 0) {
                        heatmap[y][x] += 2;
                    }
                }
            }
        }

        return heatmap;
    }

    /**
     * Get best auto-resolution strategy
     */
    public ResolutionStrategy getBestAutoResolution() {
        return resolutionStrategies.stream()
                .filter(ResolutionStrategy::isAutoApplicable)
                .max(Comparator.comparingDouble(ResolutionStrategy::getConfidence))
                .orElse(null);
    }

    /**
     * Get colliding component IDs as list
     */
    public List<String> getCollidingComponentIds() {
        return new ArrayList<>(collidingComponentIds);
    }

    /**
     * Check if specific component is involved in collision
     */
    public boolean isComponentColliding(String componentId) {
        return collidingComponentIds.contains(componentId);
    }

    /**
     * Get collision severity level (LOW, MEDIUM, HIGH, CRITICAL)
     */
    public String getSeverityLevel() {
        if (severityScore >= 80) return "CRITICAL";
        if (severityScore >= 60) return "HIGH";
        if (severityScore >= 30) return "MEDIUM";
        return "LOW";
    }

    /**
     * Get collision summary
     */
    public String getCollisionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Grid Collision Details\n");
        sb.append("======================\n");
        sb.append("Collision ID: ").append(collisionId).append("\n");
        sb.append("Component: ").append(componentId).append("\n");
        sb.append("Attempted: ").append(attemptedBounds != null ? attemptedBounds.getBoundsString() : "N/A").append("\n");
        sb.append("Collisions: ").append(collisionCount).append("\n");
        sb.append("Overlap Area: ").append(String.format("%.0f", overlapArea)).append(" cells\n");
        sb.append("Overlap %: ").append(String.format("%.1f%%", overlapPercentage)).append("\n");
        sb.append("Severity: ").append(getSeverityLevel()).append(" (").append(String.format("%.1f", severityScore)).append(")\n");

        if (!collidingBounds.isEmpty()) {
            sb.append("\nColliding Components:\n");
            for (CollisionBounds bounds : collidingBounds) {
                sb.append("  - ").append(bounds.toString()).append("\n");
            }
        }

        if (!resolutionStrategies.isEmpty()) {
            sb.append("\nResolution Strategies:\n");
            for (ResolutionStrategy strategy : resolutionStrategies) {
                sb.append("  - ").append(strategy.getSummary()).append("\n");
            }
        }

        if (hasCircularCollision) {
            sb.append("\n⚠️ CIRCULAR COLLISION DETECTED: ");
            sb.append(String.join(" → ", circularCollisionPath)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Get visual collision map as string
     */
    public String getVisualCollisionMap() {
        if (collisionHeatmap == null || collisionHeatmap.length == 0) {
            return "No collision map available";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Collision Heatmap (rows: ").append(gridRows).append(", cols: ").append(gridColumns).append(")\n");
        sb.append("Legend: .=empty, 1=attempted, 2=collision, 3=both\n");
        sb.append("┌").append("─".repeat(gridColumns * 2)).append("┐\n");

        for (int y = 0; y < Math.min(gridRows, 20); y++) { // Limit to 20 rows for readability
            sb.append("│");
            for (int x = 0; x < gridColumns; x++) {
                int val = collisionHeatmap[y][x];
                char c;
                if (val >= 3) c = '█';
                else if (val == 2) c = '▓';
                else if (val == 1) c = '▒';
                else c = '·';
                sb.append(c).append(" ");
            }
            sb.append("│\n");
        }

        sb.append("└").append("─".repeat(gridColumns * 2)).append("┘\n");
        return sb.toString();
    }

    /**
     * Get compact log representation
     */
    public String getCompactLog() {
        return String.format("GridCollision[%s] comp=%s, collisions=%d, overlap=%.1f%%, severity=%s, autoResolvable=%s",
                collisionId, componentId, collisionCount, overlapPercentage,
                getSeverityLevel(), autoResolvable);
    }

    /**
     * Get detailed error report
     */
    public Map<String, Object> getErrorReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("collisionId", collisionId);
        report.put("componentId", componentId);
        report.put("pageId", pageId);
        report.put("sectionId", sectionId);
        report.put("workspaceId", workspaceId);
        report.put("attemptedBounds", attemptedBounds);
        report.put("collidingBounds", collidingBounds);
        report.put("collisionCount", collisionCount);
        report.put("overlapArea", overlapArea);
        report.put("overlapPercentage", overlapPercentage);
        report.put("severityScore", severityScore);
        report.put("severityLevel", getSeverityLevel());
        report.put("resolutionStrategies", resolutionStrategies);
        report.put("bestResolutionStrategy", bestResolutionStrategy);
        report.put("bestResolutionConfidence", bestResolutionConfidence);
        report.put("hasCircularCollision", hasCircularCollision);
        report.put("circularCollisionPath", circularCollisionPath);
        report.put("autoResolvable", autoResolvable);
        report.put("detectedAt", detectedAt);
        report.put("correlationId", correlationId);
        return report;
    }
}