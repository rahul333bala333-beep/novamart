package com.novamart.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart catalogue service.
 *
 * <p>Owns products, categories and brands. It knows nothing about stock levels:
 * those belong to inventory-service, and are asked for over HTTP when a product
 * page needs them. Keeping "what we sell" and "how many we have" in separate
 * services is what lets a warehouse adjustment happen without touching the
 * catalogue, and vice versa.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
