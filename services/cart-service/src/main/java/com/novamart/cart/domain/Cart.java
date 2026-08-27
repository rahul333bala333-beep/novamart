package com.novamart.cart.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(name = "uq_carts_user", columnNames = "user_id"))
public class Cart {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** One cart per user, enforced by a unique constraint rather than by convention. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Cart() {
    }

    public static Cart forUser(UUID userId) {
        Cart cart = new Cart();
        cart.id = UUID.randomUUID();
        cart.userId = userId;
        cart.createdAt = Instant.now();
        cart.updatedAt = cart.createdAt;
        return cart;
    }

    /** Adding a product already present increments it rather than duplicating the line. */
    public CartItem addOrIncrement(UUID productId, int quantity) {
        Optional<CartItem> existing = findItem(productId);
        touch();
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            return item;
        }
        CartItem item = CartItem.create(this, productId, quantity);
        items.add(item);
        return item;
    }

    public void setQuantity(UUID productId, int quantity) {
        touch();
        if (quantity <= 0) {
            items.removeIf(item -> item.getProductId().equals(productId));
            return;
        }
        findItem(productId).ifPresent(item -> item.setQuantity(quantity));
    }

    public boolean remove(UUID productId) {
        touch();
        return items.removeIf(item -> item.getProductId().equals(productId));
    }

    public void clear() {
        items.clear();
        touch();
    }

    public Optional<CartItem> findItem(UUID productId) {
        return items.stream().filter(item -> item.getProductId().equals(productId)).findFirst();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
