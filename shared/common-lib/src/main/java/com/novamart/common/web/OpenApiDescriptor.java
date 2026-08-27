package com.novamart.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Builds the per-service Swagger document.
 *
 * <p>Each service publishes only its own slice of the platform API at
 * {@code /swagger-ui.html}. The authoritative, whole-platform contract remains
 * {@code api-contract/openapi.yaml}; these per-service documents exist so a
 * developer can exercise one service in isolation.
 */
public final class OpenApiDescriptor {

    private OpenApiDescriptor() {
    }

    public static OpenAPI forService(String title, String description) {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Nova Mart :: " + title)
                        .version("1.0.0")
                        .description(description
                                + "\n\nThe authoritative contract for the whole platform lives in "
                                + "`api-contract/openapi.yaml`. This document covers this service only.")
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste an access token obtained from POST /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
