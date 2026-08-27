package com.novamart.common.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.novamart.common.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wire format.
 *
 * Every client parses these two shapes, so their serialised form is a contract
 * rather than an implementation detail.
 */
class ApiResponseTest {

    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void aSuccessEnvelopeCarriesItsPayload() throws Exception {
        String out = json.writeValueAsString(ApiResponse.of("Product retrieved", List.of("a", "b")));

        assertThat(out).contains("\"success\":true");
        assertThat(out).contains("\"message\":\"Product retrieved\"");
        assertThat(out).contains("\"data\":[\"a\",\"b\"]");
    }

    @Test
    void aMessageOnlyResponseOmitsDataRatherThanSendingNull() throws Exception {
        // `data: null` forces every client to null-check a field that simply is
        // not part of the response.
        String out = json.writeValueAsString(ApiResponse.message("Deleted"));

        assertThat(out).contains("\"success\":true");
        assertThat(out).doesNotContain("data");
    }

    @Test
    void anErrorEnvelopeCarriesTheMachineReadableCode() throws Exception {
        String out = json.writeValueAsString(
                ErrorResponse.of(ErrorCode.PRODUCT_NOT_FOUND, "Product not found", "/api/v1/products/x"));

        assertThat(out).contains("\"success\":false");
        assertThat(out).contains("\"errorCode\":\"PRODUCT_NOT_FOUND\"");
        assertThat(out).contains("\"path\":\"/api/v1/products/x\"");
        assertThat(out).contains("timestamp");
        // fieldErrors is absent unless it applies.
        assertThat(out).doesNotContain("fieldErrors");
    }

    @Test
    void aValidationErrorCarriesPerFieldDetail() throws Exception {
        String out = json.writeValueAsString(ErrorResponse.validation(
                "Request validation failed", "/api/v1/auth/register",
                List.of(new ErrorResponse.FieldError("email", "Must be a well-formed email address"))));

        assertThat(out).contains("\"errorCode\":\"VALIDATION_FAILED\"");
        assertThat(out).contains("\"field\":\"email\"");
    }

    @Test
    void anEmptyPageStillReportsCoherentMetadata() {
        PageResponse<String> page = PageResponse.empty(12);

        // A client rendering pagination must not divide by zero or show "page 1
        // of 0" when a filter matches nothing.
        assertThat(page.content()).isEmpty();
        assertThat(page.page().totalElements()).isZero();
        assertThat(page.page().totalPages()).isZero();
        assertThat(page.page().first()).isTrue();
        assertThat(page.page().last()).isTrue();
    }
}
