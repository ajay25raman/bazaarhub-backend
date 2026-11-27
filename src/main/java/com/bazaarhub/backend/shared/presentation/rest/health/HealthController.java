package com.bazaarhub.backend.shared.presentation.rest.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Health & Monitoring", description = "Operational health checks and platform monitoring")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Platform health check", description = "Returns the operational readiness and health status of the BazaarHub backend service.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application is healthy and running")
    })
    public Map<String, Object> health() {
        return Map.of(
                "success", true,
                "application", "BazaarHub Backend",
                "status", "UP",
                "database", "Not configured"
        );
    }
}
