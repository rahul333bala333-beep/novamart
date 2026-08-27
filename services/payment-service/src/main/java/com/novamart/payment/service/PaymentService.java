package com.novamart.payment.service;

import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.common.security.AuthenticatedUser;
import com.novamart.common.security.CurrentUser;
import com.novamart.payment.domain.Payment;
import com.novamart.payment.dto.PaymentDtos.CreatePaymentRequest;
import com.novamart.payment.dto.PaymentDtos.PaymentDetailResponse;
import com.novamart.payment.dto.PaymentDtos.PaymentResponse;
import com.novamart.payment.dto.PaymentDtos.TransactionResponse;
import com.novamart.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final MockPaymentGateway gateway;

    public PaymentService(PaymentRepository payments, MockPaymentGateway gateway) {
        this.payments = payments;
        this.gateway = gateway;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        AuthenticatedUser caller = CurrentUser.require();

        // order-service calls this on behalf of a shopper, so it passes the owner
        // explicitly. A browser calling directly can only ever pay as itself.
        UUID ownerId = caller.isService() && request.userId() != null
                ? request.userId()
                : caller.id();

        // One payment per order. A retried checkout must not open a second
        // payment against an order that already has one.
        Payment existing = payments.findByOrderId(request.orderId()).orElse(null);
        if (existing != null) {
            log.info("Reusing existing payment {} for order {}", existing.getId(), request.orderId());
            return PaymentResponse.from(existing);
        }

        Payment payment = Payment.initiate(request.orderId(), ownerId, request.amount(),
                request.currency(), request.method(), gateway.newReference());
        payments.save(payment);
        log.info("Opened payment {} ({}) for order {}",
                payment.getId(), payment.getTransactionReference(), request.orderId());
        return PaymentResponse.from(payment);
    }

    /**
     * Settles a payment through the simulated gateway.
     *
     * <p>Idempotent: verifying an already-settled payment returns the existing
     * outcome instead of settling it twice. A retry after a dropped response must
     * not turn one capture into two.
     */
    @Transactional
    public PaymentResponse verify(UUID paymentId, boolean simulateFailure) {
        Payment payment = load(paymentId);
        assertVisibleTo(payment);

        if (payment.isSettled()) {
            log.info("Payment {} already {}; returning existing outcome",
                    paymentId, payment.getStatus());
            return PaymentResponse.from(payment);
        }

        MockPaymentGateway.Outcome outcome = gateway.settle(payment, simulateFailure);
        switch (outcome.result()) {
            case APPROVED -> payment.markSuccessful(outcome.gatewayReference());
            case DECLINED -> payment.markFailed(outcome.gatewayReference(), outcome.message());
            case DEFERRED -> {
                // Intentionally left INITIATED. Cash on delivery is captured at
                // handover, and pretending otherwise would report revenue that
                // has not been collected.
            }
        }
        return PaymentResponse.from(payment);
    }

    @Transactional
    public PaymentResponse refund(UUID paymentId, String reason) {
        Payment payment = load(paymentId);
        payment.markRefunded(reason == null ? "Order cancelled" : reason);
        log.info("Refunded payment {}", paymentId);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDetailResponse detail(UUID paymentId) {
        Payment payment = payments.findWithTransactionsById(paymentId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        assertVisibleTo(payment);
        return new PaymentDetailResponse(
                PaymentResponse.from(payment),
                payment.getTransactions().stream().map(TransactionResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PaymentResponse byOrder(UUID orderId) {
        Payment payment = payments.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
        assertVisibleTo(payment);
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(Payment.Status status, Pageable pageable) {
        return PageResponse.from(payments.findFiltered(status, pageable), PaymentResponse::from);
    }

    private Payment load(UUID id) {
        return payments.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * A shopper may only see their own payments. Answering 404 rather than 403
     * for someone else's payment avoids confirming that the id exists at all.
     */
    private void assertVisibleTo(Payment payment) {
        AuthenticatedUser caller = CurrentUser.require();
        if (!caller.canActOnBehalfOf(payment.getUserId())) {
            throw new ApiException(ErrorCode.PAYMENT_NOT_FOUND);
        }
    }
}
