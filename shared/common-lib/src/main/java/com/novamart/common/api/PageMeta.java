package com.novamart.common.api;

/**
 * Pagination descriptor returned alongside every paged collection.
 *
 * <p>Kept separate from the items so that {@code content} is always a plain
 * array, which keeps client-side typing simple.
 */
public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
