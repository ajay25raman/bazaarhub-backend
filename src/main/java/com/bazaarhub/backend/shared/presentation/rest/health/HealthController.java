package com.bazaarhub.backend.shared.presentation.rest.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller for BazaarHub platform monitoring.
 * Located in shared presentation layer to decouple operational health monitoring
 * from business domain modules.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "success", true,
                "application", "BazaarHub Backend",
                "status", "UP",
                "database", "Not configured"
        );
    }
}
