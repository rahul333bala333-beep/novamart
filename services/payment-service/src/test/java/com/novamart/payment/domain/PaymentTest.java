package com.novamart.payment.domain;

import com.novamart.common.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    private static Payment initiated() {
        return Payment.initiate(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("2499.00"), "INR", Payment.Method.MOCK_CARD, "NMPAY-ABCDEFGH");
    }

    @Test
    void openingAPaymentRecordsAnAuthorisation() {
        Payment payment = initiated();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.INITIATED);
        assertThat(payment.getTransactions())
                .extracting(Transaction::getType)
                .containsExactly(Transaction.Type.AUTHORIZE);
    }

    @Test
    void capturingAppendsToTheTrail() {
        Payment payment = initiated();
        payment.markSuccessful("GW-ABCDEFGH");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCESS);
        assertThat(payment.getSettledAt()).isNotNull();
        assertThat(payment.getTransactions())
                .extracting(Transaction::getType)
                .containsExactly(Transaction.Type.AUTHORIZE, Transaction.Type.CAPTURE);
    }

    @Test
    void aSettledPaymentCannotBeSettledAgain() {
        Payment payment = initiated();
        payment.markSuccessful("GW-1");

        // Without this, a retried verify would capture the same amount twice.
        assertThatThrownBy(() -> payment.markSuccessful("GW-2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already SUCCESS");
    }

    @Test
    void aFailedPaymentCannotBeRefunded() {
        Payment payment = initiated();
        payment.markFailed("GW-1", "Declined");

        // Refunding money that was never taken would create a real loss.
        assertThatThrownBy(() -> payment.markRefunded("customer cancelled"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only a captured payment can be refunded");
    }

    @Test
    void refundingACapturedPaymentIsRecorded() {
        Payment payment = initiated();
        payment.markSuccessful("GW-1");
        payment.markRefunded("Order cancelled");

        assertThat(payment.getStatus()).isEqualTo(Payment.Status.REFUNDED);
        assertThat(payment.getTransactions())
                .extracting(Transaction::getType)
                .containsExactly(Transaction.Type.AUTHORIZE, Transaction.Type.CAPTURE, Transaction.Type.REFUND);
    }
}
