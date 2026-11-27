package com.bazaarhub.backend.analytics.presentation.rest;

import com.bazaarhub.backend.analytics.application.dto.request.LogAnalyticsEventRequest;
import com.bazaarhub.backend.analytics.application.service.AnalyticsEventService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/analytics")
@Tag(name = "Analytics Event Ingestion", description = "Public / client-side behavioral event tracking endpoint")
public class PublicAnalyticsEventController {

    private final AnalyticsEventService eventService;

    public PublicAnalyticsEventController(AnalyticsEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/events")
    @Operation(summary = "Track analytics event", description = "Asynchronously ingests client-side user actions (e.g. PAGE_VIEW, PRODUCT_VIEW, SEARCH, ADD_TO_CART)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event recorded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid event payload", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> trackEvent(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LogAnalyticsEventRequest request) {

        Long userId = principal != null ? principal.getId() : null;
        eventService.logEvent(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Event recorded successfully", null));
    }
}
