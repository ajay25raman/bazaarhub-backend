package com.bazaarhub.backend.bootstrap.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Database and persistence configuration for BazaarHub Backend.
 *
 * <p>Enforces consistent UTC timezone across the JVM and Hibernate JDBC drivers
 * to guarantee deterministic timestamp handling across distributed marketplace nodes.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @PostConstruct
    public void configureTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("[DATABASE CONFIG] Application and JVM default timezone initialized to UTC.");
    }
}
