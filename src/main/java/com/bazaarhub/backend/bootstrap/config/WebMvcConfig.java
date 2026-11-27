package com.bazaarhub.backend.bootstrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for global web settings.
 * Configures global pagination constraints to protect endpoints against memory exhaustion.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Customizes Spring Data PageableHandlerMethodArgumentResolver to enforce:
     * - Default page size of 20 (page index 0)
     * - Maximum page size of 100 (automatically clamps any requested size > 100 to 100)
     * - 0-indexed page numbering (default)
     * - Preserves standard sorting behavior
     */
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setFallbackPageable(PageRequest.of(0, 20));
            resolver.setMaxPageSize(100);
            resolver.setOneIndexedParameters(false);
        };
    }
}
