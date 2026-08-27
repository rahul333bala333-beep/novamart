package com.novamart.product.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Turns a display name into a URL-safe slug.
 *
 * <p>Accents are decomposed and stripped rather than dropped, so "Café" becomes
 * "cafe" instead of "caf". Anything left that is not a letter, digit or hyphen
 * collapses to a single hyphen, which keeps addresses readable and stable.
 */
final class Slugs {

    private Slugs() {
    }

    static String of(String input) {
        String normalised = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalised.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "item" : slug;
    }
}
