package com.novamart.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The success envelope every endpoint returns.
 *
 * <p>Having one shape for every successful response means a client writes its
 * parsing logic once. {@code data} is omitted entirely when there is nothing to
 * return, rather than being serialised as {@code null}.
 *
 * @param success always {@code true}; present so clients can branch on a single
 *                field without inspecting the HTTP status
 * @param message short human-readable summary, safe to surface in a UI
 * @param data    the operation-specific payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, message, null);
    }
}
