package com.bazaarhub.backend.support.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.support.application.dto.request.AddTicketMessageRequest;
import com.bazaarhub.backend.support.application.dto.request.AssignTicketRequest;
import com.bazaarhub.backend.support.application.dto.request.AttachmentUploadRequest;
import com.bazaarhub.backend.support.application.dto.request.UpdateTicketPriorityRequest;
import com.bazaarhub.backend.support.application.dto.request.UpdateTicketStatusRequest;
import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.application.dto.response.TicketStatsResponse;
import com.bazaarhub.backend.support.application.service.SupportTicketService;
import com.bazaarhub.backend.support.domain.model.TicketCategory;
import com.bazaarhub.backend.support.domain.model.TicketPriority;
import com.bazaarhub.backend.support.domain.model.TicketStatus;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/support/tickets")
@Tag(name = "Admin Support & Helpdesk Desk", description = "Administrative oversight for ticket triage, agent assignment, private internal notes, and SLA tracking")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    public AdminSupportController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping
    @Operation(summary = "Query all support tickets", description = "Retrieves a paginated list of all system tickets with multi-criteria filtering across category, status, priority, order, user, and agent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Support tickets retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> queryTickets(
            @Parameter(description = "Filter by author user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by order ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "Filter by ticket category") @RequestParam(required = false) TicketCategory category,
            @Parameter(description = "Filter by ticket status") @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filter by priority level") @RequestParam(required = false) TicketPriority priority,
            @Parameter(description = "Filter by assigned agent user ID") @RequestParam(required = false) Long assignedToId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SupportTicketResponse> response = supportTicketService.getAdminTickets(
                category, status, priority, assignedToId, orderId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Support tickets retrieved", response));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get ticket lifecycle metrics", description = "Returns aggregated count of tickets across lifecycle states (OPEN, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, CLOSED).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket stats retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content)
    })
    public ResponseEntity<ApiResponse<TicketStatsResponse>> getTicketStats() {
        TicketStatsResponse stats = supportTicketService.getTicketStats();
        return ResponseEntity.ok(ApiResponse.ok("Ticket stats retrieved", stats));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get complete ticket details", description = "Retrieves full details of a ticket including assigned agent and SLA timestamps.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketDetails(
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id) {

        SupportTicketResponse response = supportTicketService.getTicketDetails(id, null, true);
        return ResponseEntity.ok(ApiResponse.ok("Ticket details retrieved", response));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign ticket to support agent", description = "Assigns or reassigns the ticket to an agent user and automatically moves ticket status from OPEN to IN_PROGRESS.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket assigned successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket or agent user not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assignTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody AssignTicketRequest request) {

        SupportTicketResponse response = supportTicketService.assignTicket(id, request.getAssignedToUserId(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Ticket assigned successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update ticket lifecycle status", description = "Transitions ticket status to IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED, or CLOSED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket status updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Invalid ticket status transition", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updateStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {

        SupportTicketResponse response = supportTicketService.updateStatus(id, request, principal.getId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Ticket status updated successfully", response));
    }

    @PatchMapping("/{id}/priority")
    @Operation(summary = "Update ticket priority", description = "Escalates or de-escalates ticket urgency (LOW, MEDIUM, HIGH, URGENT).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket priority updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> updatePriority(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody UpdateTicketPriorityRequest request) {

        SupportTicketResponse response = supportTicketService.updatePriority(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Ticket priority updated successfully", response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Post agent reply or private internal note", description = "Appends a message to the ticket. If isInternal is true, the message is a private note visible only to support agents and admins.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Support message posted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportMessageResponse>> addMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody AddTicketMessageRequest request) {

        SupportMessageResponse response = supportTicketService.addMessage(id, request, principal.getId(), true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Support message posted successfully", response));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get all ticket messages (including internal notes)", description = "Retrieves complete conversation timeline for staff, including private internal agent notes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket messages retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SupportMessageResponse>>> getTicketMessages(
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<SupportMessageResponse> response = supportTicketService.getTicketMessages(id, null, true, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Ticket messages retrieved", response));
    }

    @PostMapping("/attachments")
    @Operation(summary = "Upload staff attachment", description = "Uploads an attachment to Cloudinary for support response.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Attachment storage error", content = @Content)
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AttachmentUploadRequest request) {

        String rawImage = request.getImage();
        String uploadedUrl = supportTicketService.uploadAttachment(rawImage, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded successfully", Map.of("attachmentUrl", uploadedUrl != null ? uploadedUrl : "")));
    }
}
