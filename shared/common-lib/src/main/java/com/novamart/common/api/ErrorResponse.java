package com.novamart.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.novamart.common.error.ErrorCode;

import java.time.Instant;
import java.util.List;

/**
 * The failure envelope every endpoint returns.
 *
 * <p>Clients branch on {@link #errorCode()}, never on {@link #message()}: the
 * code is a stable contract while the message is free to be reworded or
 * translated. Stack traces and internal exception text never reach this object;
 * see {@code GlobalExceptionHandler}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        String errorCode,
        Instant timestamp,
        String path,
        List<FieldError> fieldErrors) {

    public static ErrorResponse of(ErrorCode code, String message, String path) {
        return new ErrorResponse(false, message, code.name(), Instant.now(), path, null);
    }

    public static ErrorResponse validation(String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(false, message, ErrorCode.VALIDATION_FAILED.name(),
                Instant.now(), path, fieldErrors);
    }

    /** One rejected input, addressed by the request field it came from. */
    public record FieldError(String field, String message) {
    }
}
