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
 * One image in a product's gallery.
 *
 * <p>A child table rather than a delimited string column, so an image can be
 * reordered or removed without rewriting and re-parsing the whole list, and so
 * the database can enforce that every row belongs to a real product.
 */
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_images_product"))
    private Product product;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int position;

    protected ProductImage() {
    }

    static ProductImage create(Product product, String url, int position) {
        ProductImage image = new ProductImage();
        image.id = UUID.randomUUID();
        image.product = product;
        image.url = url;
        image.position = position;
        return image;
    }

    public String getUrl() {
        return url;
    }

    public int getPosition() {
        return position;
    }
}
