package com.novamart.order.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return OpenApiDescriptor.forService("Order Service",
                "Owns orders and orchestrates the checkout saga across cart, product, "
                        + "inventory, payment and notification services. Sole owner of `order_db`.");
    }
}
