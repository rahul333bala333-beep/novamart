package com.novamart.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The commercial rules applied at checkout.
 *
 * <p>Isolated from the orchestrator so that "what does delivery cost" is
 * answerable, and testable, without standing up five services. The thresholds
 * are configuration rather than constants because they are the kind of number a
 * business changes for a weekend promotion.
 */
@Component
public class PricingPolicy {

    private final BigDecimal freeDeliveryThreshold;
    private final BigDecimal deliveryFee;

    public PricingPolicy(
            @Value("${novamart.pricing.free-delivery-threshold:999.00}") BigDecimal freeDeliveryThreshold,
            @Value("${novamart.pricing.delivery-fee:79.00}") BigDecimal deliveryFee) {
        this.freeDeliveryThreshold = freeDeliveryThreshold;
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal deliveryFeeFor(BigDecimal subtotal) {
        return subtotal.compareTo(freeDeliveryThreshold) >= 0 ? BigDecimal.ZERO : deliveryFee;
    }

    /**
     * Order-level discount.
     *
     * <p>Always zero today. There is no promotions or coupon service in this
     * platform, and returning a fabricated discount to make the order summary
     * look busier would be inventing functionality that does not exist. The field
     * is carried through the contract because a discount line is where a
     * promotions service would attach, and the storefront hides the row when it
     * is zero.
     */
    public BigDecimal discountFor(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    public BigDecimal freeDeliveryThreshold() {
        return freeDeliveryThreshold;
    }
}
