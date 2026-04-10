package com.purehome.uicore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ============================================================================
 * FAANG-ULTRA COMPRESSION REQUEST DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Adaptive Compression Strategy (ACS)
 * - Dynamically selects optimal compression algorithm based on data characteristics
 * - Implements multi-level compression with progressive refinement
 * - Supports dictionary-based compression for repeated patterns
 * - Provides delta compression for version chains
 * - Includes compression quality tuning with automatic parameter optimization
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ultra compression request with adaptive strategy selection")
public class CompressionRequest {

    // =========================================================================
    // COMPRESSION TARGET
    // =========================================================================

    @Schema(description = "Page ID to compress (mutually exclusive with workspace_id)", example = "page_123")
    @JsonProperty("page_id")
    private String pageId;

    @Schema(description = "Workspace ID to compress all pages", example = "workspace_456")
    @JsonProperty("workspace_id")
    private String workspaceId;

    @Schema(description = "Specific snapshot IDs to compress", example = "[\"snap_001\", \"snap_002\"]")
    @JsonProperty("snapshot_ids")
    private List<String> snapshotIds;

    @Schema(description = "Compression scope (PAGE, WORKSPACE, SNAPSHOTS, ALL)", example = "PAGE")
    @JsonProperty("scope")
    @Builder.Default
    private String scope = "PAGE";

    // =========================================================================
    // COMPRESSION ALGORITHM
    // =========================================================================

    @Schema(description = "Compression algorithm",
            example = "ZSTD",
            allowableValues = {"ZSTD", "LZ4", "GZIP", "SNAPPY", "DELTA", "AUTO"})
    @JsonProperty("algorithm")
    @Builder.Default
    private String algorithm = "AUTO";

    @Schema(description = "Compression level (1-22 for ZSTD, 1-9 for GZIP)", example = "19")
    @JsonProperty("level")
    private Integer level;

    @Schema(description = "Enable dictionary compression", example = "true")
    @JsonProperty("use_dictionary")
    @Builder.Default
    private Boolean useDictionary = true;

    @Schema(description = "Dictionary size in KB", example = "32")
    @JsonProperty("dictionary_size_kb")
    private Integer dictionarySizeKb;

    @Schema(description = "Enable delta compression for version chains", example = "true")
    @JsonProperty("delta_compression")
    @Builder.Default
    private Boolean deltaCompression = true;

    @Schema(description = "Delta compression window size", example = "10")
    @JsonProperty("delta_window_size")
    private Integer deltaWindowSize;

    // =========================================================================
    // COMPRESSION STRATEGY
    // =========================================================================

    @Schema(description = "Compression strategy",
            example = "BALANCED",
            allowableValues = {"SPEED", "BALANCED", "SIZE", "ULTRA"})
    @JsonProperty("strategy")
    @Builder.Default
    private String strategy = "BALANCED";

    @Schema(description = "Target compression ratio (0-1)", example = "0.7")
    @JsonProperty("target_ratio")
    private Double targetRatio;

    @Schema(description = "Maximum compression time in seconds", example = "30")
    @JsonProperty("max_time_seconds")
    private Integer maxTimeSeconds;

    @Schema(description = "Enable multi-threaded compression", example = "true")
    @JsonProperty("parallel")
    @Builder.Default
    private Boolean parallel = true;

    @Schema(description = "Number of threads for parallel compression", example = "4")
    @JsonProperty("threads")
    private Integer threads;

    // =========================================================================
    // COMPRESSION SCOPE FILTERS
    // =========================================================================

    @Schema(description = "Compress only snapshots older than days", example = "30")
    @JsonProperty("older_than_days")
    private Integer olderThanDays;

    @Schema(description = "Compress only snapshots newer than days", example = "7")
    @JsonProperty("newer_than_days")
    private Integer newerThanDays;

    @Schema(description = "Compress only snapshots by type",
            example = "[\"MANUAL\", \"AUTOMATIC\"]")
    @JsonProperty("snapshot_types")
    private List<String> snapshotTypes;

    @Schema(description = "Compress only snapshots by storage tier",
            example = "[\"HOT\", \"WARM\"]")
    @JsonProperty("storage_tiers")
    private List<String> storageTiers;

    @Schema(description = "Minimum snapshot size in bytes to compress", example = "1048576")
    @JsonProperty("min_size_bytes")
    private Long minSizeBytes;

    @Schema(description = "Maximum snapshot size in bytes to compress", example = "104857600")
    @JsonProperty("max_size_bytes")
    private Long maxSizeBytes;

    // =========================================================================
    // COMPRESSION QUALITY
    // =========================================================================

    @Schema(description = "Enable lossless compression only", example = "true")
    @JsonProperty("lossless_only")
    @Builder.Default
    private Boolean losslessOnly = true;

    @Schema(description = "Verify integrity after compression", example = "true")
    @JsonProperty("verify_integrity")
    @Builder.Default
    private Boolean verifyIntegrity = true;

    @Schema(description = "Create checksum for verification", example = "true")
    @JsonProperty("create_checksum")
    @Builder.Default
    private Boolean createChecksum = true;

    @Schema(description = "Keep original data after compression", example = "false")
    @JsonProperty("keep_original")
    @Builder.Default
    private Boolean keepOriginal = false;

    // =========================================================================
    // OPERATION MODE
    // =========================================================================

    @Schema(description = "Dry run (preview without applying)", example = "false")
    @JsonProperty("dry_run")
    @Builder.Default
    private Boolean dryRun = false;

    @Schema(description = "Force compression even if already compressed", example = "false")
    @JsonProperty("force")
    @Builder.Default
    private Boolean force = false;

    @Schema(description = "Async operation (return job ID)", example = "true")
    @JsonProperty("async")
    @Builder.Default
    private Boolean async = true;

    @Schema(description = "Notification on completion", example = "true")
    @JsonProperty("notify_on_complete")
    @Builder.Default
    private Boolean notifyOnComplete = true;

    // =========================================================================
    // ADVANCED OPTIONS
    // =========================================================================

    @Schema(description = "Enable content-aware compression", example = "true")
    @JsonProperty("content_aware")
    @Builder.Default
    private Boolean contentAware = true;

    @Schema(description = "Enable pattern detection", example = "true")
    @JsonProperty("pattern_detection")
    @Builder.Default
    private Boolean patternDetection = true;

    @Schema(description = "Custom compression dictionary ID", example = "dict_custom_001")
    @JsonProperty("dictionary_id")
    private String dictionaryId;

    @Schema(description = "Compression metadata (for tracking)")
    @JsonProperty("metadata")
    private java.util.Map<String, Object> metadata;

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Validates compression request
     */
    public ValidationResult validate() {
        List<String> errors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();

        // Validate target specification
        int targetCount = 0;
        if (pageId != null) targetCount++;
        if (workspaceId != null) targetCount++;
        if (snapshotIds != null && !snapshotIds.isEmpty()) targetCount++;

        if (targetCount == 0) {
            errors.add("Must specify compression target (page_id, workspace_id, or snapshot_ids)");
        } else if (targetCount > 1) {
            warnings.add("Multiple targets specified. Using: " +
                    (pageId != null ? "page_id" :
                            workspaceId != null ? "workspace_id" : "snapshot_ids"));
        }

        // Validate algorithm
        if (algorithm != null && !List.of("ZSTD", "LZ4", "GZIP", "SNAPPY", "DELTA", "AUTO").contains(algorithm.toUpperCase())) {
            warnings.add("Unknown algorithm: " + algorithm + ". Using AUTO");
        }

        // Validate compression level
        if (level != null) {
            if ("ZSTD".equalsIgnoreCase(algorithm) && (level < 1 || level > 22)) {
                errors.add("ZSTD level must be between 1 and 22");
            } else if ("GZIP".equalsIgnoreCase(algorithm) && (level < 1 || level > 9)) {
                errors.add("GZIP level must be between 1 and 9");
            }
        }

        // Validate strategy
        if (strategy != null && !List.of("SPEED", "BALANCED", "SIZE", "ULTRA").contains(strategy.toUpperCase())) {
            warnings.add("Invalid strategy: " + strategy + ". Using BALANCED");
        }

        // Validate target ratio
        if (targetRatio != null && (targetRatio < 0 || targetRatio > 1)) {
            errors.add("Target ratio must be between 0 and 1");
        }

        // Validate threads
        if (threads != null && threads < 1) {
            errors.add("Threads must be at least 1");
        }

        // Validate size filters
        if (minSizeBytes != null && minSizeBytes < 0) {
            errors.add("min_size_bytes cannot be negative");
        }
        if (maxSizeBytes != null && maxSizeBytes < 0) {
            errors.add("max_size_bytes cannot be negative");
        }
        if (minSizeBytes != null && maxSizeBytes != null && minSizeBytes > maxSizeBytes) {
            errors.add("min_size_bytes cannot be greater than max_size_bytes");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Gets algorithm parameters based on strategy
     */
    public CompressionParameters getAlgorithmParameters() {
        CompressionParameters params = new CompressionParameters();
        params.algorithm = algorithm;
        params.level = level;

        if (level == null) {
            switch (strategy.toUpperCase()) {
                case "SPEED":
                    params.level = "ZSTD".equalsIgnoreCase(algorithm) ? 1 : 1;
                    break;
                case "BALANCED":
                    params.level = "ZSTD".equalsIgnoreCase(algorithm) ? 10 : 5;
                    break;
                case "SIZE":
                case "ULTRA":
                    params.level = "ZSTD".equalsIgnoreCase(algorithm) ? 19 : 9;
                    break;
                default:
                    params.level = "ZSTD".equalsIgnoreCase(algorithm) ? 10 : 5;
            }
        }

        params.useDictionary = useDictionary;
        params.deltaCompression = deltaCompression;
        params.parallel = parallel;
        params.threads = threads != null ? threads : Runtime.getRuntime().availableProcessors();

        return params;
    }

    /**
     * Gets the compression target description
     */
    public String getTargetDescription() {
        if (pageId != null) return "Page: " + pageId;
        if (workspaceId != null) return "Workspace: " + workspaceId;
        if (snapshotIds != null && !snapshotIds.isEmpty()) {
            return snapshotIds.size() + " snapshots";
        }
        return "Unknown target";
    }

    /**
     * Gets the compression strategy description
     */
    public String getStrategyDescription() {
        CompressionParameters params = getAlgorithmParameters();
        return String.format("%s compression (level %d) with %s, %s, %s",
                algorithm,
                params.level,
                useDictionary ? "dictionary" : "no dictionary",
                deltaCompression ? "delta" : "full",
                parallel ? params.threads + " threads" : "single-threaded");
    }

    /**
     * Checks if this is a full workspace compression
     */
    public boolean isWorkspaceCompression() {
        return workspaceId != null && "WORKSPACE".equalsIgnoreCase(scope);
    }

    /**
     * Checks if this is a page compression
     */
    public boolean isPageCompression() {
        return pageId != null && "PAGE".equalsIgnoreCase(scope);
    }

    /**
     * Checks if this is a selective snapshot compression
     */
    public boolean isSelectiveCompression() {
        return snapshotIds != null && !snapshotIds.isEmpty();
    }

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public String getErrorMessage() { return errors.isEmpty() ? null : String.join(", ", errors); }
        public String getWarningMessage() { return warnings.isEmpty() ? null : String.join(", ", warnings); }
    }

    @Data
    public static class CompressionParameters {
        private String algorithm;
        private int level;
        private boolean useDictionary;
        private boolean deltaCompression;
        private boolean parallel;
        private int threads;
        private int dictionarySizeKb;
        private int deltaWindowSize;
    }

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a fast compression request (speed optimized)
     */
    public static CompressionRequest fastCompression(String pageId) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .algorithm("LZ4")
                .strategy("SPEED")
                .useDictionary(false)
                .deltaCompression(false)
                .parallel(true)
                .build();
    }

    /**
     * Creates a size-optimized compression request
     */
    public static CompressionRequest sizeOptimizedCompression(String pageId) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .algorithm("ZSTD")
                .level(19)
                .strategy("SIZE")
                .useDictionary(true)
                .deltaCompression(true)
                .parallel(true)
                .build();
    }

    /**
     * Creates a balanced compression request
     */
    public static CompressionRequest balancedCompression(String pageId) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .algorithm("ZSTD")
                .level(10)
                .strategy("BALANCED")
                .useDictionary(true)
                .deltaCompression(true)
                .parallel(true)
                .build();
    }

    /**
     * Creates a workspace compression request
     */
    public static CompressionRequest workspaceCompression(String workspaceId, int olderThanDays) {
        return CompressionRequest.builder()
                .workspaceId(workspaceId)
                .scope("WORKSPACE")
                .olderThanDays(olderThanDays)
                .algorithm("ZSTD")
                .level(10)
                .useDictionary(true)
                .deltaCompression(true)
                .parallel(true)
                .build();
    }

    /**
     * Creates a dry run compression preview request
     */
    public static CompressionRequest dryRunPreview(String pageId) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .dryRun(true)
                .algorithm("ZSTD")
                .level(10)
                .build();
    }

    /**
     * Creates an ultra compression request (maximum compression)
     */
    public static CompressionRequest ultraCompression(String pageId) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .algorithm("ZSTD")
                .level(22)
                .strategy("ULTRA")
                .useDictionary(true)
                .dictionarySizeKb(64)
                .deltaCompression(true)
                .deltaWindowSize(20)
                .parallel(true)
                .threads(Runtime.getRuntime().availableProcessors())
                .maxTimeSeconds(300)
                .build();
    }

    /**
     * Creates a delta-only compression request (for version chains)
     */
    public static CompressionRequest deltaCompression(String pageId, int windowSize) {
        return CompressionRequest.builder()
                .pageId(pageId)
                .algorithm("DELTA")
                .deltaCompression(true)
                .deltaWindowSize(windowSize)
                .strategy("SIZE")
                .build();
    }
}