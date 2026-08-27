package com.novamart.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart cart service.
 *
 * <p>Stores only what is genuinely the cart's own: whose it is, which products
 * are in it, and how many of each. Names, prices and stock levels are read from
 * product-service and inventory-service on every request rather than copied in
 * at add-to-cart time.
 *
 * <p>That choice costs two HTTP calls per cart read and buys correctness: a
 * cached price silently goes stale the moment an administrator edits it, and the
 * shopper would then see one figure in the cart and be charged another at
 * checkout. Prices are snapshotted exactly once, by order-service, at the moment
 * the order is placed, which is the only point where freezing them is right.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CartServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
