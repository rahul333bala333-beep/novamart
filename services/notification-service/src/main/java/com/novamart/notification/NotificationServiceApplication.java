package com.novamart.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Nova Mart notification service.
 *
 * <p>Records every transactional message the platform produces and hands it to a
 * transport for delivery.
 *
 * <p><b>The transport is a mock that writes to the service log.</b> No SMTP
 * server or SMS provider is configured for local development, and inventing
 * credentials for one would make the project unrunnable for anyone who cloned
 * it. What is real is the record: the message, its recipient, its type and its
 * timestamp are all genuinely persisted and readable through the API and the
 * admin dashboard. Only the final hop is simulated, and the API contract says so.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
