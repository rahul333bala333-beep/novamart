package com.novamart.payment.service;

import com.novamart.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * A stand-in for a real payment provider.
 *
 * <p><b>No money moves and no external system is contacted.</b> This class exists
 * so the checkout saga has a realistic component to orchestrate: something that
 * takes time, can succeed, and can fail.
 *
 * <p>The outcome is <b>deterministic, never random</b>. A gateway that failed
 * one call in ten would make the demo unreliable and the tests flaky, and a
 * flaky test is worse than no test because it trains people to re-run rather
 * than to read. So:
 *
 * <ul>
 *   <li>{@code CASH_ON_DELIVERY} is never captured up front; it stays open until
 *       the courier collects</li>
 *   <li>an explicit {@code simulateFailure} flag always declines, which is how
 *       the compensation path gets demonstrated on purpose</li>
 *   <li>everything else succeeds</li>
 * </ul>
 *
 * <p>Swapping this for a real provider means implementing the same two methods
 * against their SDK; nothing outside this class assumes the gateway is fake.
 */
@Component
public class MockPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Human-quotable reference, e.g. {@code NMPAY-7QK3M2XB}. */
    public String newReference() {
        StringBuilder sb = new StringBuilder("NMPAY-");
        for (int i = 0; i < 8; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public Outcome settle(Payment payment, boolean simulateFailure) {
        String gatewayReference = "GW-" + payment.getTransactionReference()
                .replace("NMPAY-", "").toUpperCase(Locale.ROOT);

        if (payment.getMethod() == Payment.Method.CASH_ON_DELIVERY) {
            log.info("Payment {} is cash on delivery; leaving it open until handover",
                    payment.getTransactionReference());
            return new Outcome(Result.DEFERRED, gatewayReference,
                    "Amount will be collected on delivery");
        }

        if (simulateFailure) {
            log.info("Payment {} declined by the simulated gateway (failure explicitly requested)",
                    payment.getTransactionReference());
            return new Outcome(Result.DECLINED, gatewayReference,
                    "The card issuer declined this transaction");
        }

        log.info("Payment {} captured by the simulated gateway", payment.getTransactionReference());
        return new Outcome(Result.APPROVED, gatewayReference, "Approved");
    }

    public enum Result {
        APPROVED, DECLINED,
        /** Not captured now; collected later. Used for cash on delivery. */
        DEFERRED
    }

    public record Outcome(Result result, String gatewayReference, String message) {
    }
}
