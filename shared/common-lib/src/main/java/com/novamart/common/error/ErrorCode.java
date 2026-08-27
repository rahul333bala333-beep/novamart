package com.novamart.common.error;

import org.springframework.http.HttpStatus;

/**
 * The complete error taxonomy of the platform.
 *
 * <p>Every code is paired with the HTTP status it must produce, so a handler can
 * never accidentally return {@code 200} for a failure or {@code 500} for a
 * business rule violation. Adding a case here is the only way to introduce a new
 * error, which stops the API contract and the implementation from drifting.
 */
public enum ErrorCode {

    // ---- generic ----
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "The request body could not be read"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "That HTTP method is not supported here"),
    CONFLICT(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "A service required to complete this request is unavailable"),

    // ---- auth ----
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Incorrect email or password"),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED, "This account has been disabled"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "The token is invalid"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "Address not found"),

    // ---- catalogue ----
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "Brand not found"),
    CATEGORY_NOT_EMPTY(HttpStatus.CONFLICT, "Cannot delete a category that still contains products"),
    SKU_ALREADY_EXISTS(HttpStatus.CONFLICT, "A product with this SKU already exists"),
    SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "That name is already in use"),

    // ---- cart ----
    CART_EMPTY(HttpStatus.CONFLICT, "Your cart is empty"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "That item is not in your cart"),

    // ---- orders ----
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    INVALID_ORDER_TRANSITION(HttpStatus.CONFLICT, "That status change is not allowed for this order"),

    // ---- payments ----
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "Payment was declined"),
    INVALID_PAYMENT_STATE(HttpStatus.CONFLICT, "This payment is not in a state that can be settled"),

    // ---- inventory ----
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "No stock record exists for that product"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "There is not enough stock to fulfil this request"),
    INVALID_STOCK_OPERATION(HttpStatus.CONFLICT, "That stock movement is not valid for the current quantities"),

    // ---- notifications ----
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
