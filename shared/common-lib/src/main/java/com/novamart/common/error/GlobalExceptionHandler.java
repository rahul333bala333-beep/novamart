package com.novamart.common.error;

import com.novamart.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

/**
 * Translates every exception into the documented {@link ErrorResponse} shape.
 *
 * <p>Two rules govern this class:
 *
 * <ol>
 *   <li><b>Nothing internal escapes.</b> Only the last handler sees an unexpected
 *       exception, and it logs the full trace server-side while returning a
 *       generic message. Class names, SQL fragments and stack frames are never
 *       serialised to a client.</li>
 *   <li><b>Every branch sets a meaningful status.</b> A validation failure is a
 *       400, a business rule violation is a 409, an unknown id is a 404. None of
 *       them are 500, and none of them are 200.</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        ErrorCode code = ex.errorCode();
        // Expected outcomes are logged at debug; they are not defects and should
        // not create noise in production logs.
        log.debug("Handled {} at {}: {}", code, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(ErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.validation(ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        request.getRequestURI(), fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParamValidation(ConstraintViolationException ex,
                                                               HttpServletRequest request) {
        List<ErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .sorted(Comparator.comparing(ErrorResponse.FieldError::field))
                .toList();
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.validation(ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        request.getRequestURI(), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        // The parser message can quote the offending payload, which may contain
        // user data, so it is logged rather than returned.
        log.debug("Unreadable request body at {}", request.getRequestURI(), ex);
        return status(ErrorCode.MALFORMED_REQUEST, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.validation(ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        request.getRequestURI(),
                        List.of(new ErrorResponse.FieldError(ex.getParameterName(), "This parameter is required"))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status())
                .body(ErrorResponse.validation(ErrorCode.VALIDATION_FAILED.defaultMessage(),
                        request.getRequestURI(),
                        List.of(new ErrorResponse.FieldError(ex.getName(), "Value is not in the expected format"))));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(HttpServletRequest request) {
        return status(ErrorCode.FORBIDDEN, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(HttpServletRequest request) {
        return status(ErrorCode.UNAUTHORIZED, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(HttpServletRequest request) {
        return status(ErrorCode.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpServletRequest request) {
        return status(ErrorCode.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest request) {
        // A constraint name would tell an attacker about the schema, so the
        // detail stays in the log and the client gets the generic conflict.
        log.warn("Database constraint violated at {}", request.getRequestURI(), ex);
        return status(ErrorCode.CONFLICT, request);
    }

    /**
     * Last resort. Anything reaching here is a defect, so it is logged at error
     * with the full trace and answered with a generic message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);
        return status(ErrorCode.INTERNAL_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> status(ErrorCode code, HttpServletRequest request) {
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code, code.defaultMessage(), request.getRequestURI()));
    }

    private static String lastNode(String propertyPath) {
        int idx = propertyPath.lastIndexOf('.');
        return idx < 0 ? propertyPath : propertyPath.substring(idx + 1);
    }
}
