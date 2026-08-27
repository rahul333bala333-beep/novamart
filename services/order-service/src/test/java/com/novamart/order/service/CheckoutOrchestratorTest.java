package com.novamart.order.service;

import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.order.client.CheckoutGateways;
import com.novamart.order.domain.Order;
import com.novamart.order.domain.OrderStatus;
import com.novamart.order.domain.ShippingAddress;
import com.novamart.order.dto.OrderDtos.AddressSnapshot;
import com.novamart.order.dto.OrderDtos.CartLine;
import com.novamart.order.dto.OrderDtos.CreateOrderRequest;
import com.novamart.order.dto.OrderDtos.OrderResponse;
import com.novamart.order.dto.OrderDtos.PaymentMethod;
import com.novamart.order.dto.OrderDtos.PaymentSnapshot;
import com.novamart.order.dto.OrderDtos.ProductSnapshot;
import com.novamart.order.dto.OrderDtos.UserSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The checkout saga, with every collaborating service mocked.
 *
 * Mocking is the right call here specifically because the thing under test is
 * the *choreography*: which calls happen, in what order, and what is undone when
 * a step fails. Standing up five real services would test their behaviour again
 * and make the compensation paths nearly impossible to trigger on demand.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckoutOrchestratorTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID PRODUCT_A = UUID.randomUUID();
    private static final UUID PRODUCT_B = UUID.randomUUID();
    private static final UUID ADDRESS = UUID.randomUUID();
    private static final UUID PAYMENT = UUID.randomUUID();

    @Mock
    private OrderPersistence persistence;

    @Mock
    private CheckoutGateways gateways;

    @InjectMocks
    private CheckoutOrchestrator orchestrator;

    private Order pending;

    @BeforeEach
    void setUp() {
        pending = Order.open("NM-2026-0000042", USER,
                new ShippingAddress("Home", "Ananya Iyer", "+91 98450 22002", "14 Brigade Gardens",
                        null, "Bengaluru", "Karnataka", "560034", "India"),
                "MOCK_CARD", null, null);
        pending.applyTotals(new BigDecimal("25497.00"), BigDecimal.ZERO, BigDecimal.ZERO, "INR");

        when(gateways.readCart(USER)).thenReturn(List.of(
                new CartLine(PRODUCT_A, 2),
                new CartLine(PRODUCT_B, 1)));

        when(gateways.productsByIds(any())).thenReturn(Map.of(
                PRODUCT_A, new ProductSnapshot(PRODUCT_A, "AUR-DRIFT-WHT", "Aurelia Drift Wireless Earbuds",
                        "aurelia-drift", "https://example.test/a.jpg", new BigDecimal("6499.00"), "INR"),
                PRODUCT_B, new ProductSnapshot(PRODUCT_B, "AUR-HALO-BLK", "Aurelia Halo Headphones",
                        "aurelia-halo", "https://example.test/b.jpg", new BigDecimal("18999.00"), "INR")));

        when(gateways.readAddress(USER, ADDRESS)).thenReturn(
                new AddressSnapshot("Home", "Ananya Iyer", "+91 98450 22002", "14 Brigade Gardens",
                        null, "Bengaluru", "Karnataka", "560034", "India"));

        when(gateways.readUser(USER)).thenReturn(new UserSnapshot(USER, "Ananya", "demo@novamart.dev"));

        when(persistence.findByIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(persistence.createPending(any(), any(), any(), any(), any(), any())).thenReturn(pending);
    }

    private CreateOrderRequest request(boolean simulateFailure) {
        return new CreateOrderRequest(ADDRESS, PaymentMethod.MOCK_CARD, null, simulateFailure);
    }

    // ------------------------------------------------------- happy path --

    @Test
    @DisplayName("a successful checkout reserves, pays, commits, clears the cart and confirms")
    void happyPath() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-AAAABBBB", null));
        when(gateways.verifyPayment(eq(PAYMENT), eq(false)))
                .thenReturn(new PaymentSnapshot(PAYMENT, "SUCCESS", "NMPAY-AAAABBBB", null));

        OrderResponse confirmed = confirmedResponse(OrderStatus.CONFIRMED);
        when(persistence.confirm(any(), any(), any())).thenReturn(confirmed);

        OrderResponse result = orchestrator.checkout(USER, request(false), null);

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);

        // Stock is held before payment is attempted, once per line.
        verify(gateways).reserve(eq(PRODUCT_A), eq(2), anyString());
        verify(gateways).reserve(eq(PRODUCT_B), eq(1), anyString());

        // and consumed only after payment settles.
        verify(gateways).commit(eq(PRODUCT_A), eq(2), anyString());
        verify(gateways).commit(eq(PRODUCT_B), eq(1), anyString());

        // Nothing was released, because nothing failed.
        verify(gateways, never()).releaseQuietly(any(), anyInt(), anyString());

        verify(gateways).clearCart(USER);
        verify(gateways).notifyQuietly(eq(USER), eq("ORDER_CONFIRMATION"), any(), any(), any(), any());
    }

    // --------------------------------------------------- compensation ----

    @Test
    @DisplayName("a declined payment releases every reservation and cancels the order")
    void declinedPaymentCompensates() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-AAAABBBB", null));
        when(gateways.verifyPayment(eq(PAYMENT), eq(true)))
                .thenReturn(new PaymentSnapshot(PAYMENT, "FAILED", "NMPAY-AAAABBBB",
                        "The card issuer declined this transaction"));
        when(persistence.fail(any(), any(), any())).thenReturn(confirmedResponse(OrderStatus.CANCELLED));

        assertThatThrownBy(() -> orchestrator.checkout(USER, request(true), null))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.PAYMENT_FAILED));

        // Both reservations are undone, not just the last one.
        verify(gateways).releaseQuietly(eq(PRODUCT_A), eq(2), anyString());
        verify(gateways).releaseQuietly(eq(PRODUCT_B), eq(1), anyString());

        // Nothing is consumed, and the cart is left intact so the shopper can retry.
        verify(gateways, never()).commit(any(), anyInt(), anyString());
        verify(gateways, never()).clearCart(any());

        // The failed order is still recorded, and the shopper is told.
        verify(persistence).fail(any(), eq(PAYMENT), anyString());
        verify(gateways).notifyQuietly(eq(USER), eq("PAYMENT_FAILED"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("a stock failure part-way through releases only what was already reserved")
    void partialReservationFailureReleasesWhatWasTaken() {
        // The first line succeeds, the second is out of stock.
        org.mockito.Mockito.doNothing().when(gateways).reserve(eq(PRODUCT_A), eq(2), anyString());
        org.mockito.Mockito.doThrow(new ApiException(ErrorCode.INSUFFICIENT_STOCK, "Only 0 available"))
                .when(gateways).reserve(eq(PRODUCT_B), eq(1), anyString());

        assertThatThrownBy(() -> orchestrator.checkout(USER, request(false), null))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.INSUFFICIENT_STOCK));

        // The first line must be given back, or those units are stranded forever.
        verify(gateways).releaseQuietly(eq(PRODUCT_A), eq(2), anyString());
        // The second was never held, so releasing it would be wrong.
        verify(gateways, never()).releaseQuietly(eq(PRODUCT_B), anyInt(), anyString());

        // No order is written when stock could not even be held.
        verify(persistence, never()).createPending(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("an unreachable payment service is treated as a decline, not a success")
    void paymentServiceOutageCompensates() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenThrow(new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "payment unreachable"));
        when(persistence.fail(any(), any(), any())).thenReturn(confirmedResponse(OrderStatus.CANCELLED));

        assertThatThrownBy(() -> orchestrator.checkout(USER, request(false), null))
                .isInstanceOf(ApiException.class);

        verify(gateways).releaseQuietly(eq(PRODUCT_A), eq(2), anyString());
        verify(gateways).releaseQuietly(eq(PRODUCT_B), eq(1), anyString());
        verify(gateways, never()).commit(any(), anyInt(), anyString());
    }

    // ----------------------------------------------------- guard rails ---

    @Test
    void anEmptyCartIsRefusedBeforeAnythingIsReserved() {
        when(gateways.readCart(USER)).thenReturn(List.of());

        assertThatThrownBy(() -> orchestrator.checkout(USER, request(false), null))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.CART_EMPTY));

        verify(gateways, never()).reserve(any(), anyInt(), anyString());
    }

    @Test
    void aProductThatVanishedBetweenCartAndCheckoutIsRefused() {
        // Only one of the two products still resolves.
        when(gateways.productsByIds(any())).thenReturn(Map.of(
                PRODUCT_A, new ProductSnapshot(PRODUCT_A, "SKU", "Product A", "a",
                        "https://example.test/a.jpg", new BigDecimal("100.00"), "INR")));

        assertThatThrownBy(() -> orchestrator.checkout(USER, request(false), null))
                .isInstanceOf(ApiException.class)
                .satisfies(thrown -> assertThat(((ApiException) thrown).errorCode())
                        .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));

        verify(gateways, never()).reserve(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("replaying an idempotency key returns the original order and charges nothing")
    void idempotentReplay() {
        OrderResponse original = confirmedResponse(OrderStatus.CONFIRMED);
        when(persistence.findByIdempotencyKey(USER, "key-1")).thenReturn(Optional.of(original));

        OrderResponse result = orchestrator.checkout(USER, request(false), "key-1");

        assertThat(result.id()).isEqualTo(original.id());

        // The entire saga is skipped: no stock moves and no payment is opened.
        verify(gateways, never()).reserve(any(), anyInt(), anyString());
        verify(gateways, never()).createPayment(any(), any(), any(), any(), any());
        verify(persistence, never()).createPending(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cash on delivery confirms the order without capturing payment")
    void cashOnDeliveryConfirmsWithoutCapture() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-CODCODCO", null));
        // The gateway leaves it INITIATED: collection happens at the door.
        when(gateways.verifyPayment(eq(PAYMENT), eq(false)))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-CODCODCO", null));
        when(persistence.confirm(any(), any(), any())).thenReturn(confirmedResponse(OrderStatus.CONFIRMED));

        OrderResponse result = orchestrator.checkout(USER,
                new CreateOrderRequest(ADDRESS, PaymentMethod.CASH_ON_DELIVERY, null, false), null);

        assertThat(result.status()).isEqualTo(OrderStatus.CONFIRMED);
        verify(gateways).commit(eq(PRODUCT_A), eq(2), anyString());
        verify(gateways, never()).releaseQuietly(any(), anyInt(), anyString());
    }

    @Test
    void reservationsHappenBeforeThePaymentIsOpened() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-AAAABBBB", null));
        when(gateways.verifyPayment(any(), eq(false)))
                .thenReturn(new PaymentSnapshot(PAYMENT, "SUCCESS", "NMPAY-AAAABBBB", null));
        when(persistence.confirm(any(), any(), any())).thenReturn(confirmedResponse(OrderStatus.CONFIRMED));

        orchestrator.checkout(USER, request(false), null);

        // Ordering is the point: taking money for something unavailable is worse
        // than briefly holding stock for an order that does not complete, and
        // stock must not be consumed until the money is actually captured.
        var order = org.mockito.Mockito.inOrder(gateways);
        order.verify(gateways).reserve(eq(PRODUCT_A), eq(2), anyString());
        order.verify(gateways).reserve(eq(PRODUCT_B), eq(1), anyString());
        order.verify(gateways).createPayment(any(), any(), any(), any(), any());
        order.verify(gateways).verifyPayment(eq(PAYMENT), eq(false));
        order.verify(gateways).commit(eq(PRODUCT_A), eq(2), anyString());
        order.verify(gateways).clearCart(USER);
    }

    @Test
    void thePriceChargedIsSnapshottedFromTheCatalogue() {
        when(gateways.createPayment(any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSnapshot(PAYMENT, "INITIATED", "NMPAY-AAAABBBB", null));
        when(gateways.verifyPayment(any(), eq(false)))
                .thenReturn(new PaymentSnapshot(PAYMENT, "SUCCESS", "NMPAY-AAAABBBB", null));
        when(persistence.confirm(any(), any(), any())).thenReturn(confirmedResponse(OrderStatus.CONFIRMED));

        orchestrator.checkout(USER, request(false), null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<UUID, ProductSnapshot>> captor = ArgumentCaptor.forClass(Map.class);
        verify(persistence).createPending(eq(USER), any(), any(), any(), captor.capture(), any());

        // The client never supplies a price; it comes from product-service, which
        // is what stops a tampered request from setting its own.
        assertThat(captor.getValue().get(PRODUCT_A).price()).isEqualByComparingTo("6499.00");
    }

    private OrderResponse confirmedResponse(OrderStatus status) {
        return new OrderResponse(
                pending.getId(), pending.getOrderNumber(), USER, status, List.of(),
                new BigDecimal("25497.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("25497.00"),
                "INR", null, PAYMENT, "SUCCESS", "MOCK_CARD", null, null, null, pending.getPlacedAt());
    }
}
