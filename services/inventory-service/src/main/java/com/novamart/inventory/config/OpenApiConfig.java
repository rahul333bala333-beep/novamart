package com.novamart.inventory.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenApi() {
        return OpenApiDescriptor.forService("Inventory Service",
                "Owns stock levels and the reserve / release / commit protocol that keeps "
                        + "checkout safe under concurrency. Sole owner of `inventory_db`.");
    }
}
