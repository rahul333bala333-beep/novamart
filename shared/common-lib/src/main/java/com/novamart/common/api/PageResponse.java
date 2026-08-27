package com.novamart.common.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Transport shape for a page of results.
 *
 * <p>Spring Data's own {@code Page} is deliberately not serialised directly: its
 * JSON shape is an implementation detail that has changed between Spring
 * versions, and it leaks pageable internals the API contract does not promise.
 * Mapping through this record keeps the wire format stable and owned by us.
 */
public record PageResponse<T>(List<T> content, PageMeta page) {

    /** Wraps a page whose elements are already in transport form. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), meta(page));
    }

    /** Wraps a page of entities, mapping each element to a DTO. */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), meta(page));
    }

    public static <T> PageResponse<T> empty(int size) {
        return new PageResponse<>(List.of(), new PageMeta(0, size, 0, 0, true, true));
    }

    private static PageMeta meta(Page<?> page) {
        return new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
