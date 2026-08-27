package com.novamart.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One purchased line, snapshotted at the moment of purchase.
 *
 * <p>The name, SKU, image and unit price are copied here rather than looked up
 * from product-service when the order is displayed. Three reasons, in order of
 * importance:
 *
 * <ol>
 *   <li><b>Correctness.</b> A price change next month must not rewrite what a
 *       shopper was charged last month.</li>
 *   <li><b>Durability.</b> A discontinued product still has to render on the
 *       order that bought it.</li>
 *   <li><b>Independence.</b> Order history stays readable when product-service
 *       is down.</li>
 * </ol>
 *
 * <p>{@code productId} is kept so the storefront can still offer a link back to
 * the product page when it does exist.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_items_order"))
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku", length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "slug", length = 220)
    private String slug;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    protected OrderItem() {
    }

    public static OrderItem snapshot(Order order, UUID productId, String sku, String name, String slug,
                                     String imageUrl, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.id = UUID.randomUUID();
        item.order = order;
        item.productId = productId;
        item.sku = sku;
        item.name = name;
        item.slug = slug;
        item.imageUrl = imageUrl;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return item;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
