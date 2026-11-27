package com.bazaarhub.backend.payment.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.payment.application.dto.request.CreateRazorpayOrderRequest;
import com.bazaarhub.backend.payment.application.dto.request.SelectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.VerifyRazorpayPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.response.PaymentResponse;
import com.bazaarhub.backend.payment.application.dto.response.PaymentSummaryResponse;
import com.bazaarhub.backend.payment.application.dto.response.RazorpayOrderCheckoutResponse;
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

import java.util.List;

/**
 * REST controller for customer payment workflows:
 * Razorpay order creation, signature verification, Cash on Delivery selection, and payment status checks.
 */
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Customer Payments", description = "Endpoints for initiating, verifying, and checking payments for customer orders")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PAYMENT_CREATE', 'PAYMENT_READ', 'ROLE_CUSTOMER', 'ROLE_ADMIN')")
public class CustomerPaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    public CustomerPaymentController(PaymentService paymentService, RefundService refundService) {
        this.paymentService = paymentService;
        this.refundService = refundService;
    }

    @PostMapping("/razorpay/create-order")
    @Operation(summary = "Initiate Razorpay checkout order", description = "Creates a Razorpay gateway order with server-side authoritative amount calculation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Razorpay order initiated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid order state, already paid, or cancelled", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<RazorpayOrderCheckoutResponse>> createRazorpayOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateRazorpayOrderRequest request) {
        RazorpayOrderCheckoutResponse response = paymentService.createRazorpayOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Razorpay checkout order created successfully", response));
    }

    @PostMapping("/razorpay/verify")
    @Operation(summary = "Verify Razorpay checkout signature", description = "Verifies the HMAC SHA256 signature returned by Razorpay Checkout SDK, marks payment CAPTURED, and confirms the order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment verified and captured successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Signature verification failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment record not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyRazorpayPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody VerifyRazorpayPaymentRequest request) {
        PaymentResponse response = paymentService.verifyRazorpayPayment(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified and captured successfully", response));
    }

    @PostMapping("/cod/select")
    @Operation(summary = "Select Cash on Delivery (COD)", description = "Selects Cash on Delivery as the payment method for an order and confirms the order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "COD payment configured successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COD disabled, amount exceeds limit, or already paid", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> selectCodPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SelectCodPaymentRequest request) {
        PaymentResponse response = paymentService.selectCodPayment(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Cash on Delivery selected successfully", response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details by ID", description = "Retrieves full payment details including transaction attempts for the owner or administrator.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of payment", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentId) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        PaymentResponse response = paymentService.getPaymentById(paymentId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Payment details retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment details by Order ID", description = "Retrieves payment details associated with an order for the owner or administrator.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of order", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment or order not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Payment details retrieved successfully", response));
    }

    @GetMapping
    @Operation(summary = "List customer payments", description = "Returns a paginated list of payment summaries belonging to the authenticated customer.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payments retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<PaymentSummaryResponse>>> listMyPayments(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PaymentSummaryResponse> response = paymentService.getPaymentsForCustomer(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved successfully", response));
    }

    @GetMapping("/{paymentId}/refunds")
    @Operation(summary = "List refunds for a payment", description = "Lists all refund attempts associated with a payment.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refunds retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not owner of payment", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefundsForPayment(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long paymentId) {
        boolean isAdmin = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<RefundResponse> response = refundService.getRefundsForPayment(paymentId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Refunds retrieved successfully", response));
    }

    @GetMapping("/refunds")
    @Operation(summary = "List customer refunds", description = "Returns paginated list of all refunds requested for the customer's orders.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refunds retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> listMyRefunds(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RefundResponse> response = refundService.getRefundsForCustomer(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok("Refunds retrieved successfully", response));
    }
}
