package com.novamart.auth.service;

import com.novamart.auth.domain.User;
import com.novamart.common.client.ServiceClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends the welcome message when an account is created.
 *
 * <p>Failure here is swallowed on purpose. A notification is a side effect of
 * registration, not part of it, so an unreachable notification-service must not
 * turn a successful sign-up into an error the shopper sees. The failure is
 * logged for operators instead.
 */
@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);

    private final RestClient client;

    public NotificationGateway(ServiceClientFactory factory,
                               @Value("${novamart.services.notification-url}") String baseUrl) {
        this.client = factory.create(baseUrl, "notification");
    }

    public void sendWelcome(User user) {
        try {
            client.post()
                    .uri("/api/v1/notifications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "userId", user.getId().toString(),
                            "type", "WELCOME",
                            "channel", "EMAIL",
                            "recipient", user.getEmail(),
                            "subject", "Welcome to Nova Mart",
                            "body", "Hi " + user.getFirstName()
                                    + ", your Nova Mart account is ready. "
                                    + "Browse the catalogue and enjoy free delivery over 999."))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Welcome notification for user {} could not be dispatched: {}",
                    user.getId(), ex.getMessage());
        }
    }
}
