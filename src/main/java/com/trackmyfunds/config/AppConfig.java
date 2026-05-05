package com.trackmyfunds.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AppConfig {
    // JPA auditing is enabled here so BaseEntity.createdAt / updatedAt
    // are populated automatically.
}
