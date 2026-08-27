package com.novamart.payment.dto;

import com.novamart.payment.domain.Payment;
import com.novamart.payment.domain.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {
    }

    /**
     * Opening a payment.
     *
     * <p>Note what this record does <em>not</em> contain: no card number, no
     * expiry, no CVV, no cardholder name. The simulated gateway needs none of
     * them, and a field that does not exist cannot be logged by accident.
     */
    public record CreatePaymentRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull Payment.Method method,
            UUID userId) {
    }

    public record VerifyPaymentRequest(boolean simulateFailure) {
    }

    public record PaymentResponse(
            UUID id,
            UUID orderId,
            UUID userId,
            BigDecimal amount,
            String currency,
            Payment.Method method,
            Payment.Status status,
            String transactionReference,
            String failureReason,
            Instant createdAt,
            Instant settledAt) {

        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getUserId(), p.getAmount(),
                    p.getCurrency(), p.getMethod(), p.getStatus(), p.getTransactionReference(),
                    p.getFailureReason(), p.getCreatedAt(), p.getSettledAt());
        }
    }

    public record TransactionResponse(
            UUID id,
            Transaction.Type type,
            BigDecimal amount,
            String gatewayReference,
            String message,
            Instant occurredAt) {

        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getType(), t.getAmount(),
                    t.getGatewayReference(), t.getMessage(), t.getOccurredAt());
        }
    }

    public record PaymentDetailResponse(PaymentResponse payment, List<TransactionResponse> transactions) {
    }
}
