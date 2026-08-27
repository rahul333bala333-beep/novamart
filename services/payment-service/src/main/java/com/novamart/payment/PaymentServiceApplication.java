package com.novamart.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart payment service.
 *
 * <p><b>This service simulates a payment provider. It is not connected to one.</b>
 * No real money moves, and no card number, expiry or CVV is ever accepted,
 * transmitted or stored anywhere in this codebase. Handling real card data would
 * put the application in PCI-DSS scope, which is precisely the kind of
 * requirement a demonstration project should design around rather than pretend
 * to satisfy.
 *
 * <p>The simulation is deterministic rather than random, so a demo and a test
 * produce the same outcome every time. See {@code MockPaymentGateway}.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
