package com.trackmyfunds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableJpaAuditing
public class AppConfig {
    // JPA auditing is enabled here so BaseEntity.createdAt / updatedAt
    // are populated automatically.

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
