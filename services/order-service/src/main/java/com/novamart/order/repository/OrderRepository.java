package com.novamart.order.repository;

import com.novamart.order.domain.Order;
import com.novamart.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Loads one order with its lines.
     *
     * <p>Only {@code items} is fetch-joined. Joining {@code timeline} as well
     * throws {@code MultipleBagFetchException}: Hibernate cannot fetch two
     * {@code List} collections in one query because the cartesian product makes
     * the row multiplicity ambiguous. The timeline loads lazily inside the same
     * read-only transaction, which costs one extra select for a single order.
     */
    @Query("select o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> findDetailById(@Param("id") UUID id);

    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    /**
     * Page of order ids matching the filter.
     *
     * <p>Paginating ids rather than entities is deliberate. Combining
     * {@code left join fetch} with {@code Pageable} makes Hibernate fetch every
     * matching row and paginate in memory (it warns: "applying in memory"),
     * which quietly turns a paged endpoint into a full table read. Two cheap
     * queries beat one that does not scale.
     */
    @Query("""
            select o.id from Order o
            where (:userId is null or o.userId = :userId)
              and (:status is null or o.status = :status)
            """)
    Page<UUID> findIdsFiltered(@Param("userId") UUID userId,
                               @Param("status") OrderStatus status,
                               Pageable pageable);

    /** Second step of the paged read: hydrate exactly the ids on the page. */
    @Query("select distinct o from Order o left join fetch o.items where o.id in :ids")
    List<Order> findAllWithItems(@Param("ids") Collection<UUID> ids);

    // ---- dashboard aggregates ----
    //
    // Status values are bound as enum parameters rather than written as string
    // literals. With @Enumerated(STRING) a literal like 'PENDING' is compared as
    // a string against an enum-typed path, which silently matches nothing and
    // makes the aggregate return zero instead of failing loudly.

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status not in :excluded")
    BigDecimal totalRevenue(@Param("excluded") Collection<OrderStatus> excluded);

    long countByStatus(OrderStatus status);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("select coalesce(avg(o.total), 0) from Order o where o.status not in :excluded")
    BigDecimal averageOrderValue(@Param("excluded") Collection<OrderStatus> excluded);

    /**
     * Raw rows for the revenue sparkline, grouped by day in Java.
     *
     * <p>Grouping by calendar day in JPQL needs a database-specific date
     * function, which would break the rule that every query in this project runs
     * unchanged on PostgreSQL and H2. The window is a fortnight, so the row count
     * stays small. At real volume this becomes a native query per dialect or a
     * maintained read model.
     */
    @Query("""
            select o.placedAt, o.total from Order o
            where o.placedAt >= :since and o.status not in :excluded
            """)
    List<Object[]> revenueRowsSince(@Param("since") Instant since,
                                    @Param("excluded") Collection<OrderStatus> excluded);
}
