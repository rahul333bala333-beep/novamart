package com.novamart.auth.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return OpenApiDescriptor.forService("Auth Service",
                "Owns identity for the platform: registration, sign-in, JWT issuing and "
                        + "rotation, profiles and the address book. Sole owner of `auth_db`.");
    }
}
