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
@RequestMapping("/api/v1/delivery/tickets")
@Tag(name = "Delivery Partner Support & Tickets", description = "Endpoints for delivery personnel to report courier routing issues, customer unreachable, and delivery obstacles")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('ROLE_DELIVERY', 'ROLE_ADMIN')")
public class DeliverySupportController {

    private final SupportTicketService supportTicketService;

    public DeliverySupportController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    @Operation(summary = "Submit delivery partner support ticket", description = "Creates a new ticket regarding transit obstacles, customer unreachable, or delivery address issues.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Delivery support ticket submitted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY role", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> createTicket(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateTicketRequest request) {

        SupportTicketResponse response = supportTicketService.createTicket(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Delivery partner ticket submitted successfully", response));
    }

    @GetMapping
    @Operation(summary = "List delivery partner tickets", description = "Retrieves tickets logged by the authenticated delivery personnel.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery tickets retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY role", content = @Content)
    })
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> listDeliveryTickets(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Filter by ticket status") @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SupportTicketResponse> response = supportTicketService.getUserTickets(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Delivery partner tickets retrieved", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery ticket details", description = "Retrieves details of a delivery personnel's own support ticket.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery ticket details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Ticket belongs to another user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicketDetails(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Ticket ID", example = "1") @PathVariable("id") Long id) {

        SupportTicketResponse response = supportTicketService.getTicketDetails(id, principal.getId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Delivery ticket details retrieved", response));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Reply to delivery ticket", description = "Appends an update or proof of delivery obstacle to the ticket.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reply posted successfully",
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

        request.setInternal(false);
        SupportMessageResponse response = supportTicketService.addMessage(id, request, principal.getId(), false);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reply posted successfully", response));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get delivery ticket messages", description = "Retrieves conversation messages for a delivery partner ticket.")
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
    @Operation(summary = "Close delivery ticket", description = "Allows delivery partner to close ticket once the delivery exception is cleared.")
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
                .resolutionNotes("Closed by delivery partner")
                .build();
        SupportTicketResponse response = supportTicketService.updateStatus(id, request, principal.getId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Ticket closed successfully", response));
    }

    @PostMapping("/attachments")
    @Operation(summary = "Upload delivery obstacle proof", description = "Uploads door locked photo, street view, or damaged package proof to Cloudinary.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Attachment uploaded successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires DELIVERY role", content = @Content),
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
