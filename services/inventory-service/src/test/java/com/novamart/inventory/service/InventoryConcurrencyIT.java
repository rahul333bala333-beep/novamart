package com.novamart.inventory.service;

import com.novamart.common.error.ApiException;
import com.novamart.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The oversell test.
 *
 * This is the single most important test in the platform, because it is the one
 * bug that costs real money: two shoppers reaching checkout at the same moment
 * for the last unit in stock. Without a row lock both read "1 available", both
 * decide they may proceed, and the shop sells an item it does not have.
 *
 * The threads are released together from a latch so they genuinely contend
 * rather than running one after the other and passing for the wrong reason.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyIT {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventory;

    @Test
    @DisplayName("only one of two simultaneous reservations can take the last unit")
    void concurrentReservationsCannotOversell() throws Exception {
        UUID productId = UUID.randomUUID();
        inventoryService.upsert(productId, 1, 0);

        int contenders = 8;
        CountDownLatch releaseAll = new CountDownLatch(1);
        CountDownLatch everyoneReady = new CountDownLatch(contenders);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(contenders);
        try {
            List<Callable<Void>> attempts = new java.util.ArrayList<>();
            for (int i = 0; i < contenders; i++) {
                attempts.add(() -> {
                    everyoneReady.countDown();
                    releaseAll.await(5, TimeUnit.SECONDS);
                    try {
                        inventoryService.reserve(productId, 1, "concurrency-test");
                        succeeded.incrementAndGet();
                    } catch (ApiException expected) {
                        // INSUFFICIENT_STOCK for everyone who lost the race.
                        refused.incrementAndGet();
                    } catch (RuntimeException lockContention) {
                        // A lock timeout is also a refusal, not a sale.
                        refused.incrementAndGet();
                    }
                    return null;
                });
            }

            for (Callable<Void> attempt : attempts) {
                pool.submit(attempt);
            }
            everyoneReady.await(5, TimeUnit.SECONDS);
            releaseAll.countDown();

            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            if (!pool.isShutdown()) {
                pool.shutdownNow();
            }
        }

        assertThat(succeeded.get())
                .as("exactly one reservation may win the last unit")
                .isEqualTo(1);
        assertThat(refused.get()).isEqualTo(contenders - 1);

        var item = inventory.findById(productId).orElseThrow();
        assertThat(item.getReservedQuantity()).isEqualTo(1);
        assertThat(item.availableQuantity()).isZero();
        // The invariant that must never break, whatever happened above.
        assertThat(item.getReservedQuantity()).isLessThanOrEqualTo(item.getTotalQuantity());
    }

    @Test
    @DisplayName("the reserve / commit / release cycle always balances")
    void stockIsConservedAcrossTheFullCycle() {
        UUID productId = UUID.randomUUID();
        inventoryService.upsert(productId, 20, 5);

        inventoryService.reserve(productId, 5, "order-1");
        inventoryService.reserve(productId, 3, "order-2");

        var afterReservations = inventory.findById(productId).orElseThrow();
        assertThat(afterReservations.getTotalQuantity()).isEqualTo(20);
        assertThat(afterReservations.availableQuantity()).isEqualTo(12);

        inventoryService.commit(productId, 5, "order-1");   // paid, stock leaves
        inventoryService.release(productId, 3, "order-2");  // failed, stock returns

        var settled = inventory.findById(productId).orElseThrow();
        assertThat(settled.getTotalQuantity()).isEqualTo(15);
        assertThat(settled.getReservedQuantity()).isZero();
        assertThat(settled.availableQuantity()).isEqualTo(15);
    }
}
