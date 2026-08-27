package com.novamart.order.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.repository.OrderNumberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/** Formats allocated counter values as {@code NM-2026-0001847}. */
@Component
public class OrderNumbers {

    private final OrderNumberRepository counters;

    public OrderNumbers(OrderNumberRepository counters) {
        this.counters = counters;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next() {
        long value = counters.lockCounter()
                .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR,
                        "Order number counter is missing; the V1 migration did not seed it"))
                .take();
        return String.format("NM-%d-%07d", Year.now().getValue(), value);
    }
}
