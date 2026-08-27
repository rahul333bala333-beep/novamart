package com.novamart.common.error;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The error taxonomy.
 *
 * Pairing each code with its HTTP status in the enum is what stops a handler
 * returning 200 for a failure or 500 for a business rule. These tests assert the
 * pairing is sane across the whole set, not just the ones in use today.
 */
class ErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeIsAnErrorStatus(ErrorCode code) {
        // Nothing in this enum may map to 2xx or 3xx.
        assertThat(code.status().isError())
                .as("%s maps to %s", code, code.status())
                .isTrue();
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeHasAMessageSafeToShowAShopper(ErrorCode code) {
        assertThat(code.defaultMessage()).isNotBlank();
        // Anything that looks like a leaked internal is a defect: these strings
        // are rendered directly in the interface.
        assertThat(code.defaultMessage())
                .doesNotContain("Exception")
                .doesNotContain("java.")
                .doesNotContain("org.springframework")
                .doesNotContain("SQL");
    }

    @Test
    void businessRuleViolationsAreConflictsNotServerErrors() {
        // A 500 here would tell the client to retry something that will always
        // fail, and would page an engineer for a shopper's mistake.
        assertThat(ErrorCode.INSUFFICIENT_STOCK.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.EMAIL_ALREADY_EXISTS.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.INVALID_ORDER_TRANSITION.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.CATEGORY_NOT_EMPTY.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.CART_EMPTY.status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void authenticationAndAuthorisationAreDistinct() {
        // 401 means "who are you"; 403 means "not you". Collapsing them leaves a
        // signed-out user with no idea that signing in would help.
        assertThat(ErrorCode.UNAUTHORIZED.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.INVALID_CREDENTIALS.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.TOKEN_EXPIRED.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.FORBIDDEN.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aDeclinedPaymentUsesItsOwnStatus() {
        assertThat(ErrorCode.PAYMENT_FAILED.status()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
    }

    @Test
    void onlyGenuineDefectsAreServerErrors() {
        // If this list ever grows, something that is a client's fault has been
        // misclassified as ours.
        var serverErrors = java.util.Arrays.stream(ErrorCode.values())
                .filter(code -> code.status().is5xxServerError())
                .toList();

        assertThat(serverErrors)
                .containsExactlyInAnyOrder(ErrorCode.INTERNAL_ERROR, ErrorCode.SERVICE_UNAVAILABLE);
    }
}
