package com.novamart.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart inventory service.
 *
 * <p>The authority on how many of a thing exist. Its whole reason for being a
 * separate service is that stock is the one number in a shop that several
 * actors race for: two shoppers checking out the last unit, a warehouse
 * adjustment, and a cancellation returning stock, all at once. Concentrating
 * every mutation behind one small API with one locking strategy is what makes
 * that safe.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
