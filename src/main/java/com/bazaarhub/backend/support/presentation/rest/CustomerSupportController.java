package com.bazaarhub.backend.support.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import com.bazaarhub.backend.support.application.dto.request.AddTicketMessageRequest;
import com.bazaarhub.backend.support.application.dto.request.AttachmentUploadRequest;
import com.bazaarhub.backend.support.application.dto.request.CreateTicketRequest;
import com.bazaarhub.backend.support.application.dto.request.UpdateTicketStatusRequest;
import com.bazaarhub.backend.support.application.dto.response.SupportMessageResponse;
import com.bazaarhub.backend.support.application.dto.response.SupportTicketResponse;
import com.bazaarhub.backend.support.application.service.SupportTicketService;
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
@RequestMapping("/api/v1/customer/tickets")
@Tag(name = "Customer Support & Tickets", description = "Endpoints for retail customers to create support tickets, view history, and exchange messages with support")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerSupportController {

    private final SupportTicketService supportTicketService;

    public CustomerSupportController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    @Operation(summary = "Submit support ticket", description = "Creates a new customer support ticket with an assigned tracking number and automated confirmation notification.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Support ticket submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request) {

        SupportTicketResponse response = supportTicketService.createTicket(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Support ticket submitted successfully", response));
    }

    @GetMapping
    @Operation(summary = "List customer tickets", description = "Retrieves a paginated list of tickets logged by the authenticated customer, optionally filtered by status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customer tickets retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> listCustomerTickets(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by ticket status") @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SupportTicketResponse> response = supportTicketService.getUserTickets(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Customer tickets retrieved", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details", description = "Retrieves full details of a customer's own support ticket. Throws 403 Forbidden if not the ticket owner.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Ticket belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketDetails(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id) {

        SupportTicketResponse response = supportTicketService.getTicketDetails(id, principal.getId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Ticket details retrieved", response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to ticket", description = "Appends a new message or inquiry update to the customer's open ticket conversation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Message posted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Ticket belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportMessageResponse>> addMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody AddTicketMessageRequest request) {

        // Enforce internal = false for customers
        request.setInternal(false);
        SupportMessageResponse response = supportTicketService.addMessage(id, request, principal.getId(), false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message posted successfully", response));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get ticket messages", description = "Retrieves the public conversation timeline for a support ticket. Excludes internal staff notes.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket messages retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Ticket belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SupportMessageResponse>>> getTicketMessages(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<SupportMessageResponse> response = supportTicketService.getTicketMessages(id, principal.getId(), false, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Ticket messages retrieved", response));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Close ticket", description = "Allows customer to close their own support ticket if their inquiry is satisfied.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket closed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Ticket belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> closeTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id) {

        UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                .status(TicketStatus.CLOSED)
                .resolutionNotes("Closed by customer")
                .build();
        SupportTicketResponse response = supportTicketService.updateStatus(id, request, principal.getId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Ticket closed successfully", response));
    }

    @PostMapping("/attachments")
    @Operation(summary = "Upload support attachment", description = "Uploads a screenshot, proof of defect, or document to Cloudinary and returns the public asset URL.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires CUSTOMER role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Attachment upload error", content = @Content)
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAttachment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AttachmentUploadRequest request) {

        String rawImage = request.getImage();
        String uploadedUrl = supportTicketService.uploadAttachment(rawImage, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Attachment uploaded successfully", Map.of("attachmentUrl", uploadedUrl != null ? uploadedUrl : "")));
    }
}
