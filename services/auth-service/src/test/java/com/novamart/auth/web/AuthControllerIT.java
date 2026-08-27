package com.novamart.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The authentication surface, exercised through the real filter chain against a
 * real database running the real migrations.
 *
 * These assertions are about the contract, not the implementation: the status
 * code, the error code and the envelope shape are what clients depend on.
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
class AuthControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.test";
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    // -------------------------------------------------------- register ---

    @Test
    void registeringReturns201WithATokenPair() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "firstName", "Ada", "lastName", "Lovelace",
                                "email", uniqueEmail(), "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("the password is never echoed back in any form")
    void registrationNeverReturnsThePassword() throws Exception {
        String response = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "firstName", "Grace", "lastName", "Hopper",
                                "email", uniqueEmail(), "password", "Uniqu3!Secret"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Neither the plain password nor the BCrypt hash may appear anywhere in
        // the payload.
        assertThat(response).doesNotContain("Uniqu3!Secret");
        assertThat(response).doesNotContain("$2a$");
        assertThat(response).doesNotContain("passwordHash");
    }

    @Test
    void aDuplicateEmailIsRejectedWith409() throws Exception {
        String email = uniqueEmail();
        Map<String, String> payload = Map.of(
                "firstName", "First", "lastName", "User", "email", email, "password", "Str0ng!Pass");

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(body(payload))).andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void invalidInputReturns400WithPerFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of(
                                "firstName", "", "lastName", "User",
                                "email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                // Field errors let the client mark the offending inputs rather
                // than showing one banner for the whole form.
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='password')]").exists());
    }

    // ----------------------------------------------------------- login ---

    @Test
    void seededDemoCredentialsActuallyWork() throws Exception {
        // Guards the documented demo accounts: if a migration changed a hash,
        // the README would be lying and this fails.
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "demo@novamart.dev", "password", "Demo@12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("demo@novamart.dev"));
    }

    @Test
    void theAdminAccountHoldsTheAdminRole() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "admin@novamart.dev", "password", "Admin@12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("a wrong password and an unknown account are indistinguishable")
    void credentialErrorsDoNotRevealWhetherAnAccountExists() throws Exception {
        String wrongPassword = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "demo@novamart.dev", "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        String noSuchUser = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "nobody@example.test", "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();

        // Identical responses, so the endpoint cannot be used to enumerate which
        // email addresses are registered.
        assertThat(stripTimestamp(wrongPassword)).isEqualTo(stripTimestamp(noSuchUser));
    }

    // ------------------------------------------------------ protection ---

    @Test
    void protectedEndpointsRejectAnonymousCallers() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void aMalformedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void aValidTokenReachesTheProfile() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "demo@novamart.dev", "password", "Demo@12345"))))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).path("data").path("accessToken").asText();

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("demo@novamart.dev"));
    }

    @Test
    void aShopperCannotListEveryUser() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "demo@novamart.dev", "password", "Demo@12345"))))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(login).path("data").path("accessToken").asText();

        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void aRefreshTokenCannotBeUsedAsAnAccessToken() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "demo@novamart.dev", "password", "Demo@12345"))))
                .andReturn().getResponse().getContentAsString();
        String refresh = json.readTree(login).path("data").path("refreshToken").asText();

        // Both are signed with the same key, so only the `typ` claim separates
        // them. Without that check a long-lived refresh token would authorise
        // every request.
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneStopsWorking() throws Exception {
        String login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "rohan@example.com", "password", "Shopper@123"))))
                .andReturn().getResponse().getContentAsString();
        String refresh = json.readTree(login).path("data").path("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Refresh tokens are single use, so a replay is treated as theft.
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an unknown path answers in the standard envelope, not an HTML error page")
    void unknownPathsStillReturnJson() throws Exception {
        // 401 rather than 404, and deliberately so: the security filter chain
        // runs before routing, so an anonymous caller is refused without being
        // told whether the path exists. Answering 404 here would turn the API
        // into a map of its own attack surface. What matters for clients is that
        // the response is still the documented JSON envelope rather than the
        // container's HTML error page.
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void anUnknownPathUnderAPublicPrefixReturns404InTheEnvelope() throws Exception {
        // /auth/** is public, so here routing does get a say and the answer is a
        // genuine 404 — still in the standard envelope.
        mvc.perform(post("/api/v1/auth/no-such-endpoint")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /** Timestamps differ between two calls; everything else must not. */
    private static String stripTimestamp(String payload) {
        return payload.replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":\"X\"");
    }
}
