package com.novamart.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A label/value row in the specifications table on a product page.
 *
 * <p>Modelled relationally rather than as a JSON column. Specifications vary by
 * category but are always flat pairs, and a real table keeps the migrations
 * portable across PostgreSQL and H2, which a {@code jsonb} column would not be.
 */
@Entity
@Table(name = "product_specifications")
public class ProductSpecification {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_specifications_product"))
    private Product product;

    @Column(name = "label", nullable = false, length = 80)
    private String label;

    @Column(name = "spec_value", nullable = false, length = 300)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int position;

    protected ProductSpecification() {
    }

    static ProductSpecification create(Product product, String label, String value, int position) {
        ProductSpecification spec = new ProductSpecification();
        spec.id = UUID.randomUUID();
        spec.product = product;
        spec.label = label;
        spec.value = value;
        spec.position = position;
        return spec;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public int getPosition() {
        return position;
    }

    /** Transport shape used when replacing a product's specification list. */
    public record Pair(String label, String value) {
    }
}
