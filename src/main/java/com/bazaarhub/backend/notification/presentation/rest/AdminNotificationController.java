package com.bazaarhub.backend.notification.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.notification.application.dto.request.CreateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.request.SendNotificationRequest;
import com.bazaarhub.backend.notification.application.dto.request.UpdateNotificationTemplateRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.application.dto.response.NotificationTemplateResponse;
import com.bazaarhub.backend.notification.application.service.NotificationService;
import com.bazaarhub.backend.notification.application.service.NotificationTemplateService;
import com.bazaarhub.backend.notification.domain.model.DeliveryStatus;
import com.bazaarhub.backend.notification.domain.model.NotificationChannel;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@Tag(name = "Admin Notifications & Templates", description = "Administrative oversight for notification dispatch, system logs, and notification templates")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final NotificationTemplateService templateService;

    public AdminNotificationController(NotificationService notificationService,
                                       NotificationTemplateService templateService) {
        this.notificationService = notificationService;
        this.templateService = templateService;
    }

    @GetMapping
    @Operation(summary = "Query all notifications", description = "Retrieves a paginated list of all system notifications with optional filters by userId, channel, and status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAllNotifications(
            @Parameter(description = "Filter by target user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by notification channel") @RequestParam(required = false) NotificationChannel channel,
            @Parameter(description = "Filter by delivery status") @RequestParam(required = false) DeliveryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationResponse> response = notificationService.getAllNotificationsAdmin(channel, status, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved successfully", response));
    }

    @PostMapping({"/send", "/test"})
    @Operation(summary = "Send or test notification", description = "Manually dispatches an isolated notification across any channel (IN_APP, PUSH, EMAIL).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Notification dispatched successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid recipient details", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification dispatched successfully", response));
    }

    @GetMapping("/templates")
    @Operation(summary = "List notification templates", description = "Retrieves a paginated list of notification templates configured in the system.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification templates retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<NotificationTemplateResponse>>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationTemplateResponse> response = templateService.getAllTemplates(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Notification templates retrieved", response));
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "Get notification template by ID", description = "Retrieves details of a specific notification template.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getTemplate(
            @Parameter(description = "Template ID", example = "1") @PathVariable("id") Long id) {

        NotificationTemplateResponse response = templateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.ok("Template retrieved successfully", response));
    }

    @PostMapping("/templates")
    @Operation(summary = "Create notification template", description = "Registers a new notification template for a specific channel.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Template created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Template code already exists", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateNotificationTemplateRequest request) {

        Long adminId = principal != null ? principal.getId() : null;
        NotificationTemplateResponse response = templateService.createTemplate(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Template created successfully", response));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update notification template", description = "Updates content, subject, or active status of an existing template.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Template updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Template not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Template ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody UpdateNotificationTemplateRequest request) {

        Long adminId = principal != null ? principal.getId() : null;
        NotificationTemplateResponse response = templateService.updateTemplate(id, request, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Template updated successfully", response));
    }
}
