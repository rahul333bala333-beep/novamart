package com.novamart.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brands", uniqueConstraints = @UniqueConstraint(name = "uq_brands_slug", columnNames = "slug"))
public class Brand {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Brand() {
    }

    public static Brand create(String name, String slug) {
        Brand brand = new Brand();
        brand.id = UUID.randomUUID();
        brand.name = name;
        brand.slug = slug;
        brand.createdAt = Instant.now();
        return brand;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
