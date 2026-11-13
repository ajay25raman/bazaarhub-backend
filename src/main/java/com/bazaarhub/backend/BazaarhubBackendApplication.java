package com.bazaarhub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for BazaarHub Backend.
 * Root package: com.bazaarhub.backend
 * Architecture: Clean Architecture with Modular Monolith
 */
@SpringBootApplication
public class BazaarhubBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BazaarhubBackendApplication.class, args);
    }
}
