package com.novamart.common.error;

/**
 * The single exception type services throw for expected failures.
 *
 * <p>Carrying an {@link ErrorCode} rather than an HTTP status means the service
 * layer never imports anything web-related: it states what went wrong in domain
 * terms, and the handler decides how that maps onto HTTP.
 *
 * <p>The stack trace is deliberately not captured. These represent anticipated
 * outcomes such as "product not found", not defects, and they are thrown often
 * enough on hot paths that filling in a trace is measurable waste.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public ApiException(ErrorCode errorCode, String message) {
        super(message, null, false, false);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public static ApiException of(ErrorCode code) {
        return new ApiException(code);
    }

    public static ApiException of(ErrorCode code, String message) {
        return new ApiException(code, message);
    }

    public static ApiException forbidden() {
        return new ApiException(ErrorCode.FORBIDDEN);
    }

    public static ApiException unauthorized() {
        return new ApiException(ErrorCode.UNAUTHORIZED);
    }
}
