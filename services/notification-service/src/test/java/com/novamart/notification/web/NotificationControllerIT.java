package com.novamart.notification.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novamart.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Notification recording and, most importantly, its visibility rules: a shopper
 * must see only their own messages.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
/**
 * Each test runs in a transaction that is rolled back afterwards.
 *
 * Without this the tests are order-dependent: one that creates a product makes
 * another that asserts "the catalogue has 25 items" fail, and which one runs
 * first is not guaranteed. Rolling back means every test sees the seeded data
 * exactly as the migrations left it, whatever ran before.
 */
class NotificationControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JwtService jwt;

    private String shopperToken(UUID userId) {
        return jwt.issueAccessToken(userId, "shopper@example.test", Set.of("USER"));
    }

    private String adminToken() {
        return jwt.issueAccessToken(UUID.randomUUID(), "admin@novamart.dev", Set.of("ADMIN"));
    }

    private void record(UUID userId, String subject) throws Exception {
        mvc.perform(post("/api/v1/notifications")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", userId.toString(),
                                "type", "ORDER_CONFIRMATION",
                                "channel", "EMAIL",
                                "recipient", "shopper@example.test",
                                "subject", subject,
                                "body", "Body text."))))
                .andExpect(status().isCreated());
    }

    @Test
    void aServiceCanRecordANotificationAndItIsMarkedSent() throws Exception {
        mvc.perform(post("/api/v1/notifications")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", UUID.randomUUID().toString(),
                                "type", "WELCOME",
                                "subject", "Welcome to Nova Mart",
                                "body", "Your account is ready."))))
                .andExpect(status().isCreated())
                // SENT means the mock transport accepted it, which is what the
                // contract says and what the admin screen explains.
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"));
    }

    @Test
    void aShopperCannotRecordANotification() throws Exception {
        mvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + shopperToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", UUID.randomUUID().toString(),
                                "type", "WELCOME",
                                "subject", "Forged",
                                "body", "Should not be allowed."))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnonymousCallerCannotListNotifications() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void aShopperSeesOnlyTheirOwnMessages() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        record(alice, "Alice order confirmed");
        record(bob, "Bob order confirmed");

        // The scope comes from the verified token, not from a parameter, so
        // there is nothing for Alice to tamper with.
        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + shopperToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.subject=='Alice order confirmed')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.subject=='Bob order confirmed')]").doesNotExist());
    }

    @Test
    void anAdministratorSeesEverything() throws Exception {
        UUID carol = UUID.randomUUID();
        record(carol, "Carol order confirmed");

        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.subject=='Carol order confirmed')]").exists());
    }

    @Test
    void invalidPayloadsAreRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/notifications")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "userId", UUID.randomUUID().toString(),
                                "type", "WELCOME",
                                "subject", "",
                                "body", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
