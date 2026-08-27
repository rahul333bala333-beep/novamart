package com.novamart.notification.config;

import com.novamart.common.web.OpenApiDescriptor;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return OpenApiDescriptor.forService("Notification Service",
                "Owns the transactional message log. Records are real and queryable; "
                        + "delivery uses a MOCK transport that writes to the service log rather "
                        + "than sending email or SMS. Sole owner of `notification_db`.");
    }
}
