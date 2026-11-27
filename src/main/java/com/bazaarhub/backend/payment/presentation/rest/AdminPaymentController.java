package com.bazaarhub.backend.payment.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.payment.application.dto.request.CollectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.application.dto.response.PaymentResponse;
import com.bazaarhub.backend.payment.application.dto.response.PaymentSummaryResponse;
import com.bazaarhub.backend.payment.application.dto.response.RefundResponse;
import com.bazaarhub.backend.payment.application.service.PaymentService;
import com.bazaarhub.backend.payment.application.service.RefundService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
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

/**
 * REST controller for administrative payment operations:
 * COD collection, refund management, and global payment/refund ledger inspection.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Payments & Refunds", description = "Endpoints for administrators to manage cash collection, initiate refunds, and audit payments")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PAYMENT_MANAGE', 'REFUND_MANAGE', 'ROLE_ADMIN')")
public class AdminPaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    public AdminPaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @PostMapping("/payments/{paymentId}/cod-collect")
    @Operation(summary = "Collect Cash on Delivery payment", description = "Marks a pending COD transaction as CAPTURED and reconciles the order's payment status to PAID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "COD payment collected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payment is not COD or already captured", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> collectCodPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentId,
            @Valid @RequestBody(required = false) CollectCodPaymentRequest request) {
        PaymentResponse response = paymentService.collectCodPayment(principal.getId(), paymentId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cash on Delivery payment collected successfully", response));
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @Operation(summary = "Initiate payment refund", description = "Executes full or partial refund against a captured payment through Razorpay or manual settlement.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Refund initiated and processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payment not refundable or requested amount exceeds balance", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentId,
            @Valid @RequestBody CreateRefundRequest request) {
        RefundResponse response = refundService.createRefund(principal.getId(), paymentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Refund processed successfully", response));
    }

    @GetMapping("/payments")
    @Operation(summary = "List all payments", description = "Returns a paginated list of all payments across all customers and orders.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<PaymentSummaryResponse>>> listAllPayments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentSummaryResponse> response = paymentService.getAllPaymentsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved successfully", response));
    }

    @GetMapping("/refunds")
    @Operation(summary = "List all refunds", description = "Returns a paginated list of all refunds in the platform.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refunds retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> listAllRefunds(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RefundResponse> response = refundService.getAllRefundsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Refunds retrieved successfully", response));
    }

    @GetMapping("/refunds/{refundId}")
    @Operation(summary = "Get refund details by ID", description = "Retrieves full details of a specific refund record.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Refund not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long refundId) {
        RefundResponse response = refundService.getRefundById(refundId, principal.getId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Refund details retrieved successfully", response));
    }
}
