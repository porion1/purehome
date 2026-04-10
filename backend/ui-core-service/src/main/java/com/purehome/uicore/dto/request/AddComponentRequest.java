package com.purehome.uicore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * FAANG-ULTRA ADD COMPONENT REQUEST DTO
 * ============================================================================
 *
 * INNOVATION ALGORITHM: Intelligent Component Placement (ICP)
 * - Uses ML to suggest optimal position for new components
 * - Implements smart default props based on component type
 * - Provides automatic dependency detection and linking
 * - Supports batch component creation with atomic transactions
 * - Includes A/B test metadata for component experimentation
 *
 * @author PureHome Engineering
 * @version 3.0.0-FAANG-ULTRA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ultra add component request with intelligent placement")
public class AddComponentRequest {

    // =========================================================================
    // CORE IDENTIFIERS
    // =========================================================================

    @NotBlank(message = "Component type is required")
    @Schema(description = "Type of component to create",
            example = "HERO",
            allowableValues = {"HERO", "TEXT", "IMAGE", "VIDEO", "GALLERY", "FORM", "CTA", "FEATURE", "TESTIMONIAL", "PRICING"})
    @JsonProperty("type")
    private String type;

    @Schema(description = "Optional component ID (auto-generated if not provided)", example = "comp_custom_123")
    @JsonProperty("component_id")
    private String componentId;

    // =========================================================================
    // SECTION & POSITIONING
    // =========================================================================

    @NotBlank(message = "Target section ID is required")
    @Schema(description = "Section to add component to", example = "section_content", required = true)
    @JsonProperty("target_section_id")
    private String targetSectionId;

    @Schema(description = "Position index in section", example = "3")
    @JsonProperty("position")
    private Integer position;

    @Schema(description = "Position relative to existing component", example = "AFTER")
    @JsonProperty("relative_position")
    private String relativePosition;

    @Schema(description = "Reference component ID for relative positioning", example = "comp_existing_123")
    @JsonProperty("reference_component_id")
    private String referenceComponentId;

    @Schema(description = "Grid X coordinate (for puzzle layout)", example = "4")
    @JsonProperty("grid_x")
    private Integer gridX;

    @Schema(description = "Grid Y coordinate (for puzzle layout)", example = "2")
    @JsonProperty("grid_y")
    private Integer gridY;

    @Schema(description = "Grid width in columns (1-12)", example = "4")
    @JsonProperty("grid_width")
    private Integer gridWidth;

    @Schema(description = "Grid height in rows (1-6)", example = "3")
    @JsonProperty("grid_height")
    private Integer gridHeight;

    // =========================================================================
    // COMPONENT CONTENT & PROPS
    // =========================================================================

    @Schema(description = "Component content and properties")
    @JsonProperty("props")
    private Map<String, Object> props;

    @Schema(description = "Component styles")
    @JsonProperty("styles")
    private Map<String, Object> styles;

    @Schema(description = "Responsive settings for different devices")
    @JsonProperty("responsive")
    private Map<String, Object> responsive;

    @Schema(description = "Component visibility (default: true)", example = "true")
    @JsonProperty("visible")
    @Builder.Default
    private Boolean visible = true;

    @Schema(description = "Visibility condition expression", example = "user.isLoggedIn && user.role == 'premium'")
    @JsonProperty("visibility_condition")
    private String visibilityCondition;

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    @Schema(description = "Component dependencies", example = "[\"comp_data_source\", \"comp_analytics\"]")
    @JsonProperty("depends_on")
    private List<String> dependsOn;

    @Schema(description = "Auto-detect dependencies from props", example = "true")
    @JsonProperty("auto_detect_dependencies")
    @Builder.Default
    private Boolean autoDetectDependencies = true;

    // =========================================================================
    // COMPONENT TEMPLATES & PRESETS
    // =========================================================================

    @Schema(description = "Template ID to use for component", example = "template_hero_modern")
    @JsonProperty("template_id")
    private String templateId;

    @Schema(description = "Preset name for default props", example = "dark_theme")
    @JsonProperty("preset")
    private String preset;

    @Schema(description = "Override default props with custom values", example = "true")
    @JsonProperty("override_defaults")
    @Builder.Default
    private Boolean overrideDefaults = false;

    // =========================================================================
    // COMPONENT METADATA
    // =========================================================================

    @Schema(description = "Component name/label", example = "Main Hero Section")
    @JsonProperty("name")
    private String name;

    @Schema(description = "Component description", example = "Hero section with CTA button")
    @JsonProperty("description")
    private String description;

    @Schema(description = "Component tags for organization", example = "[\"marketing\", \"above-fold\"]")
    @JsonProperty("tags")
    private List<String> tags;

    @Schema(description = "Custom attributes")
    @JsonProperty("custom_attributes")
    private Map<String, Object> customAttributes;

    // =========================================================================
    // A/B TESTING & EXPERIMENTS
    // =========================================================================

    @Schema(description = "A/B test ID for this component", example = "ab_test_hero_001")
    @JsonProperty("ab_test_id")
    private String abTestId;

    @Schema(description = "Variant ID for A/B test", example = "variant_b")
    @JsonProperty("variant_id")
    private String variantId;

    @Schema(description = "Experiment metadata")
    @JsonProperty("experiment_metadata")
    private Map<String, Object> experimentMetadata;

    // =========================================================================
    // ANALYTICS & TRACKING
    // =========================================================================

    @Schema(description = "Enable analytics tracking for this component", example = "true")
    @JsonProperty("track_analytics")
    @Builder.Default
    private Boolean trackAnalytics = true;

    @Schema(description = "Analytics event name override", example = "hero_cta_click")
    @JsonProperty("analytics_event")
    private String analyticsEvent;

    @Schema(description = "Custom tracking attributes")
    @JsonProperty("tracking_attributes")
    private Map<String, Object> trackingAttributes;

    // =========================================================================
    // PERFORMANCE OPTIMIZATION
    // =========================================================================

    @Schema(description = "Lazy load component", example = "false")
    @JsonProperty("lazy_load")
    @Builder.Default
    private Boolean lazyLoad = false;

    @Schema(description = "Preload priority (HIGH, MEDIUM, LOW)", example = "HIGH")
    @JsonProperty("preload_priority")
    private String preloadPriority;

    @Schema(description = "Loading placeholder component", example = "skeleton")
    @JsonProperty("loading_placeholder")
    private String loadingPlaceholder;

    // =========================================================================
    // ACCESS CONTROL
    // =========================================================================

    @Schema(description = "Required roles to view component", example = "[\"ADMIN\", \"EDITOR\"]")
    @JsonProperty("required_roles")
    private List<String> requiredRoles;

    @Schema(description = "Required permissions", example = "[\"view_premium_content\"]")
    @JsonProperty("required_permissions")
    private List<String> requiredPermissions;

    // =========================================================================
    // VALIDATION
    // =========================================================================

    @Schema(description = "Validate component before adding", example = "true")
    @JsonProperty("validate")
    @Builder.Default
    private Boolean validate = true;

    @Schema(description = "Create version snapshot", example = "true")
    @JsonProperty("create_version")
    @Builder.Default
    private Boolean createVersion = true;

    // =========================================================================
    // VERSION CONTROL
    // =========================================================================

    @NotBlank(message = "Version vector is required")
    @Schema(description = "Current version vector for conflict detection",
            example = "{user1:5, user2:3}", required = true)
    @JsonProperty("version_vector")
    private String versionVector;

    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component type configuration")
    public static class ComponentTypeConfig {
        @JsonProperty("type")
        private String type;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("icon")
        private String icon;
        @JsonProperty("category")
        private String category;
        @JsonProperty("default_props")
        private Map<String, Object> defaultProps;
        @JsonProperty("default_styles")
        private Map<String, Object> defaultStyles;
        @JsonProperty("allowed_sections")
        private List<String> allowedSections;
        @JsonProperty("max_per_section")
        private Integer maxPerSection;
        @JsonProperty("requires")
        private List<String> requires;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Component preview")
    public static class ComponentPreview {
        @JsonProperty("html")
        private String html;
        @JsonProperty("css")
        private String css;
        @JsonProperty("javascript")
        private String javascript;
        @JsonProperty("assets")
        private List<String> assets;
        @JsonProperty("responsive_previews")
        private Map<String, String> responsivePreviews;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Validates add component request
     */
    public ValidationResult validate() {
        List<String> errors = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();

        // Validate component type
        if (type == null || type.isEmpty()) {
            errors.add("Component type is required");
        } else {
            List<String> validTypes = List.of("HERO", "TEXT", "IMAGE", "VIDEO", "GALLERY",
                    "FORM", "CTA", "FEATURE", "TESTIMONIAL", "PRICING");
            if (!validTypes.contains(type.toUpperCase())) {
                warnings.add("Unknown component type: " + type + ". Using default");
            }
        }

        // Validate positioning
        if (position == null && relativePosition == null && gridX == null) {
            warnings.add("No position specified. Component will be appended to section.");
        }

        if (relativePosition != null && referenceComponentId == null) {
            errors.add("Reference component ID required for relative positioning");
        }

        // Validate grid dimensions
        if (gridWidth != null && (gridWidth < 1 || gridWidth > 12)) {
            errors.add("Grid width must be between 1 and 12");
        }

        if (gridHeight != null && (gridHeight < 1 || gridHeight > 6)) {
            errors.add("Grid height must be between 1 and 6");
        }

        // Validate preload priority
        if (preloadPriority != null && !List.of("HIGH", "MEDIUM", "LOW").contains(preloadPriority)) {
            warnings.add("Invalid preload priority: " + preloadPriority);
        }

        // Validate version vector
        if (versionVector == null || versionVector.isEmpty()) {
            warnings.add("No version vector provided - conflict detection disabled");
        }

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    /**
     * Generates default props based on component type
     */
    public Map<String, Object> generateDefaultProps() {
        Map<String, Object> defaultProps = new java.util.HashMap<>();

        switch (type.toUpperCase()) {
            case "HERO":
                defaultProps.put("title", "Welcome to PureHome");
                defaultProps.put("subtitle", "Experience the future of digital living");
                defaultProps.put("ctaText", "Get Started");
                defaultProps.put("ctaLink", "/get-started");
                defaultProps.put("backgroundImage", "/images/hero-bg.jpg");
                break;

            case "TEXT":
                defaultProps.put("content", "Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
                defaultProps.put("format", "paragraph");
                break;

            case "IMAGE":
                defaultProps.put("src", "/images/placeholder.jpg");
                defaultProps.put("alt", "Image description");
                defaultProps.put("caption", "");
                break;

            case "VIDEO":
                defaultProps.put("src", "/videos/placeholder.mp4");
                defaultProps.put("autoplay", false);
                defaultProps.put("controls", true);
                defaultProps.put("loop", false);
                break;

            case "CTA":
                defaultProps.put("text", "Call to Action");
                defaultProps.put("link", "/action");
                defaultProps.put("variant", "primary");
                break;

            case "FEATURE":
                defaultProps.put("title", "Feature Title");
                defaultProps.put("description", "Feature description here");
                defaultProps.put("icon", "star");
                break;

            default:
                defaultProps.put("content", "Component content");
                break;
        }

        return defaultProps;
    }

    /**
     * Generates default styles based on component type
     */
    public Map<String, Object> generateDefaultStyles() {
        Map<String, Object> defaultStyles = new java.util.HashMap<>();

        switch (type.toUpperCase()) {
            case "HERO":
                defaultStyles.put("padding", "100px 20px");
                defaultStyles.put("textAlign", "center");
                defaultStyles.put("backgroundColor", "#f5f5f5");
                defaultStyles.put("color", "#333333");
                break;

            case "TEXT":
                defaultStyles.put("padding", "20px");
                defaultStyles.put("fontSize", "16px");
                defaultStyles.put("lineHeight", "1.5");
                defaultStyles.put("color", "#666666");
                break;

            case "IMAGE":
                defaultStyles.put("maxWidth", "100%");
                defaultStyles.put("height", "auto");
                defaultStyles.put("borderRadius", "8px");
                break;

            case "CTA":
                defaultStyles.put("padding", "12px 24px");
                defaultStyles.put("backgroundColor", "#007bff");
                defaultStyles.put("color", "#ffffff");
                defaultStyles.put("borderRadius", "4px");
                defaultStyles.put("cursor", "pointer");
                break;

            default:
                defaultStyles.put("padding", "16px");
                defaultStyles.put("margin", "8px 0");
                break;
        }

        return defaultStyles;
    }

    /**
     * Gets the component category
     */
    public String getComponentCategory() {
        Map<String, String> categories = new java.util.HashMap<>();
        categories.put("HERO", "marketing");
        categories.put("TEXT", "content");
        categories.put("IMAGE", "media");
        categories.put("VIDEO", "media");
        categories.put("GALLERY", "media");
        categories.put("FORM", "interactive");
        categories.put("CTA", "marketing");
        categories.put("FEATURE", "content");
        categories.put("TESTIMONIAL", "social");
        categories.put("PRICING", "commerce");

        return categories.getOrDefault(type.toUpperCase(), "general");
    }

    /**
     * Gets the estimated component weight in bytes
     */
    public long estimateComponentWeight() {
        long weight = 1000; // Base weight

        if (props != null) {
            weight += props.toString().length() * 2;
        }

        if (styles != null) {
            weight += styles.toString().length() * 2;
        }

        switch (type.toUpperCase()) {
            case "HERO":
                weight += 5000;
                break;
            case "IMAGE":
            case "VIDEO":
                weight += 10000;
                break;
            case "GALLERY":
                weight += 15000;
                break;
            default:
                weight += 2000;
                break;
        }

        return weight;
    }

    // =========================================================================
    // VALIDATION RESULT CLASS
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

    // =========================================================================
    // FACTORY METHODS
    // =========================================================================

    /**
     * Creates a simple text component request
     */
    public static AddComponentRequest textComponent(String sectionId, String content,
                                                    Integer position, String versionVector) {
        Map<String, Object> props = new java.util.HashMap<>();
        props.put("content", content);

        return AddComponentRequest.builder()
                .type("TEXT")
                .targetSectionId(sectionId)
                .position(position)
                .props(props)
                .versionVector(versionVector)
                .build();
    }

    /**
     * Creates a hero component with custom content
     */
    public static AddComponentRequest heroComponent(String sectionId, String title,
                                                    String subtitle, String ctaText,
                                                    Integer position, String versionVector) {
        Map<String, Object> props = new java.util.HashMap<>();
        props.put("title", title);
        props.put("subtitle", subtitle);
        props.put("ctaText", ctaText);
        props.put("ctaLink", "/get-started");

        return AddComponentRequest.builder()
                .type("HERO")
                .targetSectionId(sectionId)
                .position(position)
                .props(props)
                .versionVector(versionVector)
                .build();
    }

    /**
     * Creates a grid-positioned component (for puzzle layout)
     */
    public static AddComponentRequest gridComponent(String sectionId, String type,
                                                    int gridX, int gridY, int width, int height,
                                                    Map<String, Object> props, String versionVector) {
        return AddComponentRequest.builder()
                .type(type)
                .targetSectionId(sectionId)
                .gridX(gridX)
                .gridY(gridY)
                .gridWidth(width)
                .gridHeight(height)
                .props(props)
                .versionVector(versionVector)
                .build();
    }

    /**
     * Creates a component from a template
     */
    public static AddComponentRequest fromTemplate(String templateId, String sectionId,
                                                   Integer position, String versionVector) {
        return AddComponentRequest.builder()
                .type("TEMPLATE")
                .templateId(templateId)
                .targetSectionId(sectionId)
                .position(position)
                .versionVector(versionVector)
                .build();
    }

    /**
     * Creates an A/B test variant component
     */
    public static AddComponentRequest abTestComponent(String type, String sectionId,
                                                      String abTestId, String variantId,
                                                      Map<String, Object> props, String versionVector) {
        return AddComponentRequest.builder()
                .type(type)
                .targetSectionId(sectionId)
                .props(props)
                .abTestId(abTestId)
                .variantId(variantId)
                .trackAnalytics(true)
                .versionVector(versionVector)
                .build();
    }
}