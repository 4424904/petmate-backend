package com.petmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
// @EnableJpaRepositories(basePackages = {"com.petmate.repository.jpa",
// "com.petmate.repository.jpa.test"})
@EnableJpaRepositories(basePackages = { "com.petmate.repository.jpa", "com.petmate.repository.jpa.test",
        "com.petmate.payment.repository.jpa" })
public class JpaConfig {
}