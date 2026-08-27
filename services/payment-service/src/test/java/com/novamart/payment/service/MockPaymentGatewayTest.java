package com.novamart.payment.service;

import com.novamart.payment.domain.Payment;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The simulated gateway.
 *
 * The property that matters is determinism. A gateway that failed randomly would
 * make every test that touches checkout flaky, and a flaky test trains people to
 * re-run rather than to read.
 */
class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    private static Payment payment(Payment.Method method) {
        return Payment.initiate(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1999.00"), "INR", method, "NMPAY-TESTREF1");
    }

    @RepeatedTest(20)
    void cardPaymentsAlwaysSucceedUnlessFailureIsRequested() {
        var outcome = gateway.settle(payment(Payment.Method.MOCK_CARD), false);
        assertThat(outcome.result()).isEqualTo(MockPaymentGateway.Result.APPROVED);
    }

    @RepeatedTest(20)
    void requestingFailureAlwaysDeclines() {
        var outcome = gateway.settle(payment(Payment.Method.MOCK_CARD), true);
        assertThat(outcome.result()).isEqualTo(MockPaymentGateway.Result.DECLINED);
        assertThat(outcome.message()).isNotBlank();
    }

    @Test
    void cashOnDeliveryIsDeferredRatherThanCaptured() {
        // Reporting this as APPROVED would book revenue that has not been
        // collected; the courier has not been to the door yet.
        var outcome = gateway.settle(payment(Payment.Method.CASH_ON_DELIVERY), false);
        assertThat(outcome.result()).isEqualTo(MockPaymentGateway.Result.DEFERRED);
    }

    @Test
    void cashOnDeliveryIgnoresTheFailureSwitch() {
        var outcome = gateway.settle(payment(Payment.Method.CASH_ON_DELIVERY), true);
        assertThat(outcome.result()).isEqualTo(MockPaymentGateway.Result.DEFERRED);
    }

    @Test
    void referencesAreUniqueAndReadable() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String reference = gateway.newReference();
            assertThat(reference).startsWith("NMPAY-").hasSize(14);
            // No I, O, 0 or 1 in the alphabet, so a reference read over the
            // phone cannot be transcribed ambiguously.
            assertThat(reference.substring(6)).doesNotContain("O", "I", "0", "1");
            seen.add(reference);
        }
        assertThat(seen).hasSize(500);
    }
}
