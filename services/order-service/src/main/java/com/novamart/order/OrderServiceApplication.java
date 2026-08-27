package com.novamart.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart order service.
 *
 * <p>Owns orders, and orchestrates checkout across five other services. It is the
 * only service in the platform that coordinates rather than merely serves, which
 * makes it the one place where the distributed-transaction problem has to be
 * confronted honestly: there is no database transaction spanning cart_db,
 * inventory_db, payment_db and order_db, so consistency is achieved with a saga
 * and explicit compensation instead. See {@code CheckoutOrchestrator}.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
