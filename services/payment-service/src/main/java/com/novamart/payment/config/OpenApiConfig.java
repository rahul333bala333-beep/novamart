package com.novamart.payment.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return OpenApiDescriptor.forService("Payment Service",
                "Owns payments and their transaction trail against a SIMULATED gateway. "
                        + "No real provider is integrated and no card data is ever accepted "
                        + "or stored. Sole owner of `payment_db`.");
    }
}
