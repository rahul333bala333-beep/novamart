package com.novamart.order.service;

import com.novamart.common.api.PageMeta;
import com.novamart.common.api.PageResponse;
import com.novamart.common.error.ApiException;
import com.novamart.common.error.ErrorCode;
import com.novamart.common.security.AuthenticatedUser;
import com.novamart.common.security.CurrentUser;
import com.novamart.order.client.CheckoutGateways;
import com.novamart.order.domain.Order;
import com.novamart.order.domain.OrderItem;
import com.novamart.order.domain.OrderStatus;
import com.novamart.order.dto.OrderDtos.DailyRevenue;
import com.novamart.order.dto.OrderDtos.OrderDetailResponse;
import com.novamart.order.dto.OrderDtos.OrderResponse;
import com.novamart.order.dto.OrderDtos.OrderStatsResponse;
import com.novamart.order.dto.OrderDtos.StatusCount;
import com.novamart.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Order retrieval, lifecycle changes and dashboard aggregates. */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int REVENUE_WINDOW_DAYS = 14;

    /**
     * Statuses excluded from revenue.
     *
     * <p>PENDING has not been paid for and CANCELLED has been refunded, so
     * counting either would overstate takings. Bound as enum parameters rather
     * than written as JPQL string literals: against an {@code @Enumerated(STRING)}
     * path a literal comparison silently matches nothing and the aggregate
     * quietly returns zero.
     */
    private static final Set<OrderStatus> NON_REVENUE =
            Set.of(OrderStatus.PENDING, OrderStatus.CANCELLED);

    private final OrderRepository orders;
    private final CheckoutGateways gateways;

    public OrderService(OrderRepository orders, CheckoutGateways gateways) {
        this.orders = orders;
        this.gateways = gateways;
    }

    /**
     * Paged order list, read in two steps.
     *
     * <p>First a page of ids, then one query that hydrates exactly those ids with
     * their lines. Fetch-joining a collection directly onto a {@code Pageable}
     * query forces Hibernate to paginate in memory after loading every matching
     * row, which turns a paged endpoint into a full scan without saying so.
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(OrderStatus status, UUID userIdFilter, Pageable pageable) {
        AuthenticatedUser caller = CurrentUser.require();
        // A shopper is always scoped to themselves; the userId parameter is
        // honoured only for an administrator, so it cannot be used to read
        // another account's orders.
        UUID scope = caller.isAdmin() ? userIdFilter : caller.id();

        Page<UUID> ids = orders.findIdsFiltered(scope, status, pageable);
        if (ids.isEmpty()) {
            return new PageResponse<>(List.of(), meta(ids));
        }

        Map<UUID, Order> loaded = orders.findAllWithItems(ids.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));

        // Re-apply the page order: `in :ids` does not preserve it.
        List<OrderResponse> content = ids.getContent().stream()
                .map(loaded::get)
                .filter(java.util.Objects::nonNull)
                .map(OrderResponse::from)
                .toList();

        return new PageResponse<>(content, meta(ids));
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse detail(UUID orderId) {
        Order order = orders.findDetailById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
        assertVisible(order);
        // The timeline is a second collection and cannot be fetch-joined
        // alongside items; touching it here loads it inside this transaction.
        order.getTimeline().size();
        return OrderDetailResponse.from(order);
    }

    /**
     * Cancels an order and unwinds what it consumed.
     *
     * <p>The state change is validated first, so an attempt to cancel a shipped
     * order fails before any stock is returned or money refunded.
     */
    @Transactional
    public OrderResponse cancel(UUID orderId, String reason) {
        Order order = orders.findDetailById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
        assertVisible(order);

        String note = reason == null || reason.isBlank() ? "Cancelled by request" : reason;
        order.cancel(note);

        // Stock was committed at confirmation, so cancelling returns it as new
        // availability rather than releasing a reservation that no longer exists.
        for (OrderItem item : order.getItems()) {
            gateways.restockQuietly(item.getProductId(), item.getQuantity(), order.getOrderNumber());
        }
        if (order.getPaymentId() != null && "SUCCESS".equals(order.getPaymentStatus())) {
            gateways.refundQuietly(order.getPaymentId(), note);
            order.attachPayment(order.getPaymentId(), "REFUNDED");
        }

        gateways.notifyQuietly(order.getUserId(), "ORDER_CANCELLED", null,
                "Your order " + order.getOrderNumber() + " has been cancelled",
                "Order " + order.getOrderNumber() + " was cancelled. " + note
                        + (order.getPaymentId() != null ? " Any payment taken has been refunded." : ""),
                order.getId().toString());

        log.info("Order {} cancelled: {}", order.getOrderNumber(), note);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse advance(UUID orderId, OrderStatus target, String note) {
        Order order = orders.findDetailById(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));

        // Cancellation has its own path because it must also refund and restock;
        // routing it through here would skip both.
        if (target == OrderStatus.CANCELLED) {
            throw new ApiException(ErrorCode.INVALID_ORDER_TRANSITION,
                    "Use the cancel endpoint so that stock and payment are also unwound");
        }
        order.transitionTo(target, note);

        switch (target) {
            case SHIPPED -> gateways.notifyQuietly(order.getUserId(), "ORDER_SHIPPED", null,
                    "Your order " + order.getOrderNumber() + " has shipped",
                    "Good news. Order " + order.getOrderNumber() + " is on its way.",
                    order.getId().toString());
            case DELIVERED -> gateways.notifyQuietly(order.getUserId(), "ORDER_DELIVERED", null,
                    "Your order " + order.getOrderNumber() + " was delivered",
                    "Order " + order.getOrderNumber() + " has been delivered. We hope you enjoy it.",
                    order.getId().toString());
            default -> {
                // PROCESSING is an internal fulfilment step. A shopper does not
                // need an email telling them a warehouse picked their box.
            }
        }
        log.info("Order {} advanced to {}", order.getOrderNumber(), target);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse stats() {
        Instant since = Instant.now().minus(Duration.ofDays(REVENUE_WINDOW_DAYS));

        Map<LocalDate, BigDecimal> revenue = new HashMap<>();
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : orders.revenueRowsSince(since, NON_REVENUE)) {
            LocalDate day = LocalDate.ofInstant((Instant) row[0], ZoneOffset.UTC);
            revenue.merge(day, (BigDecimal) row[1], BigDecimal::add);
            counts.merge(day, 1L, Long::sum);
        }

        // Quiet days are emitted as zero rather than omitted, so the chart keeps
        // an even x-axis instead of silently compressing them.
        List<DailyRevenue> series = new ArrayList<>();
        LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        for (int i = REVENUE_WINDOW_DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            series.add(new DailyRevenue(day,
                    revenue.getOrDefault(day, BigDecimal.ZERO),
                    counts.getOrDefault(day, 0L)));
        }

        List<StatusCount> breakdown = new ArrayList<>();
        for (Object[] row : orders.countGroupedByStatus()) {
            breakdown.add(new StatusCount((OrderStatus) row[0], ((Number) row[1]).longValue()));
        }

        return new OrderStatsResponse(
                orders.totalRevenue(NON_REVENUE),
                orders.count(),
                orders.countByStatus(OrderStatus.PENDING),
                orders.countByStatus(OrderStatus.CANCELLED),
                orders.averageOrderValue(NON_REVENUE),
                series,
                breakdown);
    }

    /** 404 rather than 403 for someone else's order, so ids cannot be probed. */
    private void assertVisible(Order order) {
        AuthenticatedUser caller = CurrentUser.require();
        if (!caller.canActOnBehalfOf(order.getUserId())) {
            throw new ApiException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    private static PageMeta meta(Page<?> page) {
        return new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
