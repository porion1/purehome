package com.purehome.uicore.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASH = Pattern.compile("-+");

    public String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String normalized = Normalizer.normalize(input.toLowerCase(), Normalizer.Form.NFD);
        String withoutDiacritics = normalized.replaceAll("\\p{M}", "");
        String noWhitespace = WHITESPACE.matcher(withoutDiacritics).replaceAll("-");
        String cleaned = NON_LATIN.matcher(noWhitespace).replaceAll("");
        String slug = MULTIPLE_DASH.matcher(cleaned).replaceAll("-");

        return slug.replaceAll("^-|-$", "");
    }

    public String generateUniqueSlug(String baseSlug, String workspaceId) {
        return baseSlug + "-" + System.currentTimeMillis();
    }
}