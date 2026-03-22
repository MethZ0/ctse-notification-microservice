package com.methush.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * General application beans configuration.
 */
@Configuration
public class AppConfig {

    /**
     * RestTemplate bean used by NotificationService to call
     * the API Gateway (e.g. GET /auth/adminUsers for LOW_STOCK alerts).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
