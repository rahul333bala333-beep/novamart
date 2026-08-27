package com.novamart.product.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI productServiceOpenApi() {
        return OpenApiDescriptor.forService("Product Service",
                "Owns the catalogue: products, categories and brands, with search, "
                        + "filtering, sorting and pagination. Sole owner of `product_db`.");
    }
}
