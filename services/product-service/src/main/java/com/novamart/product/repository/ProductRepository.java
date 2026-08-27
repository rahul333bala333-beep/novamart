package com.novamart.product.repository;

import com.novamart.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * The catalogue query behind every listing, search and filter combination.
     *
     * <p>Each filter is expressed as {@code :param is null or <predicate>} so one
     * statement serves all of them. Building a query string by concatenation
     * instead is how SQL injection gets in; here every value is a bound
     * parameter and the shape of the query is fixed at compile time.
     *
     * <p><b>Two things in this query exist because of PostgreSQL specifically,
     * and both were found by running against a real server rather than H2.</b>
     *
     * <p>First, {@code cast(:search as string)}. PostgreSQL cannot infer the type
     * of a bare parameter in {@code ? is null}, so it assumes {@code bytea} and
     * the surrounding {@code lower(...)} fails with
     * {@code function lower(bytea) does not exist}. H2 accepts it. The cast tells
     * the driver what the parameter is.
     *
     * <p>Second, the explicit {@code left join}. Writing {@code p.brand.slug} in
     * the WHERE clause makes Hibernate emit an <em>inner</em> join to brands,
     * which silently drops every product whose brand is null. Since a brand is
     * optional, that meant a brandless product could never appear in the shop.
     * The join is now spelled out.
     *
     * <p>{@code left join fetch} loads category and brand alongside the products.
     * Without it, rendering a page of twelve issues twelve extra selects for the
     * category names, which is the classic N+1 and is invisible until the
     * catalogue grows. The count query omits the fetch, because counting rows
     * does not need the associations loaded.
     */
    @Query(value = """
            select p from Product p
            left join fetch p.category c
            left join fetch p.brand b
            where p.active = true
              and (cast(:search as string) is null
                   or lower(p.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(p.shortDescription) like lower(concat('%', cast(:search as string), '%')))
              and (cast(:categorySlug as string) is null or c.slug = :categorySlug)
              and (cast(:brandSlug as string) is null or b.slug = :brandSlug)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:featured is null or p.featured = :featured)
              and (:minRating is null or p.ratingAverage >= :minRating)
            """,
            countQuery = """
            select count(p) from Product p
            left join p.category c
            left join p.brand b
            where p.active = true
              and (cast(:search as string) is null
                   or lower(p.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(p.shortDescription) like lower(concat('%', cast(:search as string), '%')))
              and (cast(:categorySlug as string) is null or c.slug = :categorySlug)
              and (cast(:brandSlug as string) is null or b.slug = :brandSlug)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
              and (:featured is null or p.featured = :featured)
              and (:minRating is null or p.ratingAverage >= :minRating)
            """)
    Page<Product> search(@Param("search") String search,
                         @Param("categorySlug") String categorySlug,
                         @Param("brandSlug") String brandSlug,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("featured") Boolean featured,
                         @Param("minRating") BigDecimal minRating,
                         Pageable pageable);

    @Query("select p from Product p left join fetch p.category left join fetch p.brand "
            + "where p.slug = :slug and p.active = true")
    Optional<Product> findBySlugAndActiveTrue(@Param("slug") String slug);

    @Query("select p from Product p left join fetch p.category left join fetch p.brand "
            + "where p.id = :id and p.active = true")
    Optional<Product> findByIdAndActiveTrue(@Param("id") UUID id);

    @Query("select p from Product p left join fetch p.category left join fetch p.brand "
            + "where p.id in :ids and p.active = true")
    List<Product> findByIdInAndActiveTrue(@Param("ids") Collection<UUID> ids);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    long countByCategoryIdAndActiveTrue(UUID categoryId);

    @Query("select p.category.id, count(p) from Product p where p.active = true group by p.category.id")
    List<Object[]> countActiveByCategory();

    @Query("select p.brand.id, count(p) from Product p where p.active = true and p.brand is not null group by p.brand.id")
    List<Object[]> countActiveByBrand();
}
