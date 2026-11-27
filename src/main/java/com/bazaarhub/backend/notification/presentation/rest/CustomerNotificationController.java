package com.bazaarhub.backend.notification.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.notification.application.dto.request.NotificationPreferenceRequest;
import com.bazaarhub.backend.notification.application.dto.response.NotificationPreferenceResponse;
import com.bazaarhub.backend.notification.application.dto.response.NotificationResponse;
import com.bazaarhub.backend.notification.application.dto.response.UnreadNotificationCountResponse;
import com.bazaarhub.backend.notification.application.service.NotificationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@Tag(name = "Customer Notifications", description = "Customer notification inbox, read/unread tracking, and notification preferences")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerNotificationController {

    private final NotificationService notificationService;

    public CustomerNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    @Operation(summary = "List customer notifications", description = "Retrieves a paginated list of notification history for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getCustomerNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationResponse> response = notificationService.getUserNotifications(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved successfully", response));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Get unread notification count", description = "Returns the count of unread notifications for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread count retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadNotificationCount(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        UnreadNotificationCountResponse response = notificationService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Unread count retrieved successfully", response));
    }

    @PatchMapping("/notifications/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read. Fails if the notification does not belong to the user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Notification belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Notification ID", example = "1") @PathVariable("id") Long id) {

        NotificationResponse response = notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notification marked as read", response));
    }

    @PatchMapping("/notifications/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the authenticated customer as read.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Integer>> markAllNotificationsAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        int updatedCount = notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Marked " + updatedCount + " notifications as read", updatedCount));
    }

    @GetMapping({"/notification-preferences", "/notifications/preferences"})
    @Operation(summary = "Get customer notification preferences", description = "Retrieves communication channel preferences for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification preferences retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getNotificationPreferences(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {

        NotificationPreferenceResponse response = notificationService.getNotificationPreferences(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Notification preferences retrieved", response));
    }

    @PutMapping({"/notification-preferences", "/notifications/preferences"})
    @Operation(summary = "Update customer notification preferences", description = "Updates communication channel preferences for the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification preferences updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER or ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updateNotificationPreferences(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationPreferenceRequest request) {

        NotificationPreferenceResponse response = notificationService.updateNotificationPreferences(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Notification preferences updated", response));
    }
}
