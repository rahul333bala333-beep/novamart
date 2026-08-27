package com.novamart.cart.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cartServiceOpenApi() {
        return OpenApiDescriptor.forService("Cart Service",
                "Owns the shopper cart. Prices and stock are read live from product-service "
                        + "and inventory-service rather than cached. Sole owner of `cart_db`.");
    }
}
