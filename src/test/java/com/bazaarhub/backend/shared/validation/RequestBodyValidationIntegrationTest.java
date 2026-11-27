package com.bazaarhub.backend.shared.validation;

import com.bazaarhub.backend.cart.application.dto.request.CheckoutValidateRequest;
import com.bazaarhub.backend.delivery.application.dto.request.DeliveryProofUploadRequest;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.identity.infrastructure.security.JwtService;
import com.bazaarhub.backend.seller.application.dto.request.AdminActionRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
import com.bazaarhub.backend.support.application.dto.request.AttachmentUploadRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 4 Integration Test Suite: REM-QLTY-01
 * Verifies Request Body Validation, Structured DTO Enforcement, and Standardized HTTP 400 Error Responses
 * across all audited endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RequestBodyValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String sellerToken;
    private String customerToken;
    private String deliveryToken;

    @BeforeEach
    void setUp() {
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));
        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));
        RoleEntity deliveryRole = roleRepository.findByName("ROLE_DELIVERY")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_DELIVERY").description("Delivery").build()));

        UserEntity adminUser = getOrCreateActiveUser("val.admin@bazaarhub.com", "+919111122201", "Admin Val", adminRole);
        UserEntity customerUser = getOrCreateActiveUser("val.customer@bazaarhub.com", "+919111122202", "Customer Val", customerRole);
        UserEntity sellerUser = getOrCreateActiveUser("val.seller@bazaarhub.com", "+919111122203", "Seller Val", sellerRole);
        UserEntity deliveryUser = getOrCreateActiveUser("val.delivery@bazaarhub.com", "+919111122204", "Delivery Val", deliveryRole);

        adminToken = jwtService.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getPhone(),
                adminUser.getFullName(), List.of("ROLE_ADMIN"), List.of("ROLE_ADMIN", "SELLER_MANAGE", "ORDER_MANAGE", "SUPPORT_MANAGE"));

        customerToken = jwtService.generateAccessToken(customerUser.getId(), customerUser.getEmail(), customerUser.getPhone(),
                customerUser.getFullName(), List.of("ROLE_CUSTOMER"), List.of("ROLE_CUSTOMER", "CART_READ", "CART_UPDATE"));

        sellerToken = jwtService.generateAccessToken(sellerUser.getId(), sellerUser.getEmail(), sellerUser.getPhone(),
                sellerUser.getFullName(), List.of("ROLE_SELLER"), List.of("ROLE_SELLER", "SELLER_ORDER_MANAGE"));

        deliveryToken = jwtService.generateAccessToken(deliveryUser.getId(), deliveryUser.getEmail(), deliveryUser.getPhone(),
                deliveryUser.getFullName(), List.of("ROLE_DELIVERY"), List.of("ROLE_DELIVERY"));
    }

    private UserEntity getOrCreateActiveUser(String email, String phone, String fullName, RoleEntity role) {
        UserEntity user = userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(UserEntity.builder()
                        .email(email)
                        .phone(phone)
                        .fullName(fullName)
                        .passwordHash("hashed")
                        .accountStatus(AccountStatus.ACTIVE)
                        .emailVerified(true)
                        .build()));

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }

        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", user.getId());
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", user.getId(), role.getId());
        return user;
    }

    // =========================================================================
    // 1. ADMIN SELLER CONTROLLER: suspendSeller Validation
    // =========================================================================
    @Nested
    @DisplayName("AdminSellerController.suspendSeller Validation")
    class AdminSellerSuspendValidationTests {

        @Test
        @DisplayName("POST /api/v1/admin/sellers/{id}/suspend - Reason exceeding 500 characters returns 400 Bad Request")
        void testSuspendSeller_ReasonExceeds500Chars_Returns400() throws Exception {
            String longReason = "A".repeat(501);
            AdminActionRequest request = AdminActionRequest.builder().reason(longReason).build();

            mockMvc.perform(post("/api/v1/admin/sellers/999/suspend")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Reason must not exceed 500 characters"))));
        }

        @Test
        @DisplayName("POST /api/v1/admin/sellers/{id}/suspend - Valid reason passes validation (returns 404 for non-existent seller, not 400)")
        void testSuspendSeller_ValidReason_PassesValidation() throws Exception {
            AdminActionRequest request = AdminActionRequest.builder().reason("Legitimate suspension reason").build();

            mockMvc.perform(post("/api/v1/admin/sellers/999/suspend")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // 2. INTERNAL SHIPMENT CONTROLLER: markReadyForPickup Validation
    // =========================================================================
    @Nested
    @DisplayName("InternalShipmentController.markReadyForPickup Validation")
    class ReadyForPickupValidationTests {

        @Test
        @DisplayName("POST /api/v1/shipments/{id}/ready-for-pickup - Negative package weight returns 400")
        void testReadyForPickup_NegativeWeight_Returns400() throws Exception {
            ReadyForPickupRequest request = ReadyForPickupRequest.builder()
                    .packageWeight(new BigDecimal("-1.500"))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/999/ready-for-pickup")
                            .header("Authorization", "Bearer " + sellerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Package weight must be positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/{id}/ready-for-pickup - Negative dimensions return 400")
        void testReadyForPickup_NegativeDimensions_Returns400() throws Exception {
            ReadyForPickupRequest request = ReadyForPickupRequest.builder()
                    .packageWeight(new BigDecimal("1.500"))
                    .lengthCm(new BigDecimal("-10.0"))
                    .widthCm(new BigDecimal("-5.0"))
                    .heightCm(new BigDecimal("-2.0"))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/999/ready-for-pickup")
                            .header("Authorization", "Bearer " + sellerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Package length must be positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/{id}/ready-for-pickup - Packaging notes exceeding 500 chars returns 400")
        void testReadyForPickup_NotesExceed500Chars_Returns400() throws Exception {
            ReadyForPickupRequest request = ReadyForPickupRequest.builder()
                    .packageWeight(new BigDecimal("1.000"))
                    .notes("N".repeat(501))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/999/ready-for-pickup")
                            .header("Authorization", "Bearer " + sellerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Packaging notes must not exceed 500 characters"))));
        }
    }

    // =========================================================================
    // 3. INTERNAL SHIPMENT CONTROLLER: calculateRate (Shipping Rate Quote)
    // =========================================================================
    @Nested
    @DisplayName("InternalShipmentController.calculateRate Validation")
    class ShippingRateQuoteValidationTests {

        @Test
        @DisplayName("POST /api/v1/shipments/rate-quote - Negative weight returns 400")
        void testCalculateRate_NegativeWeight_Returns400() throws Exception {
            ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                    .weightKg(new BigDecimal("-2.500"))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Total parcel weight must be positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/rate-quote - Invalid Indian PIN codes return 400")
        void testCalculateRate_InvalidPostalCodes_Returns400() throws Exception {
            ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                    .originPostalCode("12345") // 5 digits (invalid)
                    .destinationPostalCode("012345") // Starts with 0 (invalid)
                    .build();

            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Origin postal code must be a valid 6-digit Indian PIN code"))))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Destination postal code must be a valid 6-digit Indian PIN code"))));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/rate-quote - Negative sellerOrderId and subtotal return 400")
        void testCalculateRate_NegativeOrderIdAndSubtotal_Returns400() throws Exception {
            ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                    .sellerOrderId(-10L)
                    .subtotal(new BigDecimal("-100.00"))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Seller order ID must be positive"))))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Subtotal must be zero or positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/rate-quote - Valid rate quote request succeeds with 200 OK")
        void testCalculateRate_ValidRequest_Returns200() throws Exception {
            ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                    .originPostalCode("560001")
                    .destinationPostalCode("560038")
                    .originCity("Bengaluru")
                    .destinationCity("Bengaluru")
                    .weightKg(new BigDecimal("1.500"))
                    .subtotal(new BigDecimal("750.00"))
                    .build();

            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.baseCharge").exists())
                    .andExpect(jsonPath("$.data.totalShippingCharge").exists());
        }
    }

    // =========================================================================
    // 4. SUPPORT TICKET ATTACHMENT UPLOAD VALIDATION (4 Support Controllers)
    // =========================================================================
    @Nested
    @DisplayName("Support Controllers Attachment Upload Validation")
    class SupportAttachmentValidationTests {

        @Test
        @DisplayName("POST /api/v1/customer/tickets/attachments - Blank image returns 400")
        void testCustomerSupport_BlankAttachment_Returns400() throws Exception {
            AttachmentUploadRequest request = AttachmentUploadRequest.builder().image("").build();

            mockMvc.perform(post("/api/v1/customer/tickets/attachments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Attachment image content is required"))));
        }

        @Test
        @DisplayName("POST /api/v1/customer/tickets/attachments - Empty JSON object returns 400")
        void testCustomerSupport_EmptyJson_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/customer/tickets/attachments")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Attachment image content is required"))));
        }

        @Test
        @DisplayName("POST /api/v1/admin/support/tickets/attachments - Blank image returns 400")
        void testAdminSupport_BlankAttachment_Returns400() throws Exception {
            AttachmentUploadRequest request = AttachmentUploadRequest.builder().image("   ").build();

            mockMvc.perform(post("/api/v1/admin/support/tickets/attachments")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Attachment image content is required"))));
        }

        @Test
        @DisplayName("POST /api/v1/seller/tickets/attachments - Blank image returns 400")
        void testSellerSupport_BlankAttachment_Returns400() throws Exception {
            AttachmentUploadRequest request = AttachmentUploadRequest.builder().image(null).build();

            mockMvc.perform(post("/api/v1/seller/tickets/attachments")
                            .header("Authorization", "Bearer " + sellerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Attachment image content is required"))));
        }

        @Test
        @DisplayName("POST /api/v1/delivery/tickets/attachments - Blank image returns 400")
        void testDeliverySupport_BlankAttachment_Returns400() throws Exception {
            AttachmentUploadRequest request = AttachmentUploadRequest.builder().image("").build();

            mockMvc.perform(post("/api/v1/delivery/tickets/attachments")
                            .header("Authorization", "Bearer " + deliveryToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Attachment image content is required"))));
        }
    }

    // =========================================================================
    // 5. CUSTOMER CHECKOUT CONTROLLER: validateCheckout Validation
    // =========================================================================
    @Nested
    @DisplayName("CustomerCheckoutController.validateCheckout Validation")
    class CheckoutValidateValidationTests {

        @Test
        @DisplayName("POST /api/v1/checkout/validate - Negative addressId returns 400")
        void testValidateCheckout_NegativeAddressId_Returns400() throws Exception {
            CheckoutValidateRequest request = CheckoutValidateRequest.builder().addressId(-1L).build();

            mockMvc.perform(post("/api/v1/checkout/validate")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Address ID must be positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/checkout/validate - Zero addressId returns 400")
        void testValidateCheckout_ZeroAddressId_Returns400() throws Exception {
            CheckoutValidateRequest request = CheckoutValidateRequest.builder().addressId(0L).build();

            mockMvc.perform(post("/api/v1/checkout/validate")
                            .header("Authorization", "Bearer " + customerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Address ID must be positive"))));
        }
    }

    // =========================================================================
    // 6. DELIVERY BOY CONTROLLER: uploadProof Validation
    // =========================================================================
    @Nested
    @DisplayName("DeliveryBoyController.uploadProof Validation")
    class DeliveryProofUploadValidationTests {

        @Test
        @DisplayName("POST /api/v1/delivery/proof/upload - Blank image returns 400")
        void testUploadProof_BlankImage_Returns400() throws Exception {
            DeliveryProofUploadRequest request = DeliveryProofUploadRequest.builder()
                    .image("")
                    .build();

            mockMvc.perform(post("/api/v1/delivery/proof/upload")
                            .header("Authorization", "Bearer " + deliveryToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Proof image content is required"))));
        }

        @Test
        @DisplayName("POST /api/v1/delivery/proof/upload - Negative shipmentId returns 400")
        void testUploadProof_NegativeShipmentId_Returns400() throws Exception {
            DeliveryProofUploadRequest request = DeliveryProofUploadRequest.builder()
                    .shipmentId(-5L)
                    .image("https://example.com/proof.jpg")
                    .build();

            mockMvc.perform(post("/api/v1/delivery/proof/upload")
                            .header("Authorization", "Bearer " + deliveryToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Shipment ID must be positive"))));
        }

        @Test
        @DisplayName("POST /api/v1/delivery/proof/upload - Folder name exceeding 200 chars returns 400")
        void testUploadProof_FolderExceeds200Chars_Returns400() throws Exception {
            DeliveryProofUploadRequest request = DeliveryProofUploadRequest.builder()
                    .image("https://example.com/proof.jpg")
                    .folder("F".repeat(201))
                    .build();

            mockMvc.perform(post("/api/v1/delivery/proof/upload")
                            .header("Authorization", "Bearer " + deliveryToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("Folder name must not exceed 200 characters"))));
        }
    }

    // =========================================================================
    // 7. PAYMENT WEBHOOK: Critical Webhook Exception & Protection
    // =========================================================================
    @Nested
    @DisplayName("PaymentWebhookController.handleRazorpayWebhook Validation")
    class RazorpayWebhookProtectionTests {

        @Test
        @DisplayName("POST /api/v1/payments/webhooks/razorpay - Missing signature header returns 400")
        void testWebhook_MissingSignatureHeader_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"event\":\"payment.captured\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("Webhook signature header 'X-Razorpay-Signature' is required")));
        }

        @Test
        @DisplayName("POST /api/v1/payments/webhooks/razorpay - Blank signature header returns 400")
        void testWebhook_BlankSignatureHeader_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                            .header("X-Razorpay-Signature", "   ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"event\":\"payment.captured\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("Webhook signature header 'X-Razorpay-Signature' is required")));
        }

        @Test
        @DisplayName("POST /api/v1/payments/webhooks/razorpay - Blank body returns 400")
        void testWebhook_BlankBody_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                            .header("X-Razorpay-Signature", "some_sig")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("Webhook payload is required")));
        }

        @Test
        @DisplayName("POST /api/v1/payments/webhooks/razorpay - Payload exceeding 64KB returns 400")
        void testWebhook_PayloadExceeds64KB_Returns400() throws Exception {
            String hugePayload = "{\"data\":\"" + "X".repeat(70000) + "\"}";

            mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                            .header("X-Razorpay-Signature", "some_sig")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(hugePayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("Webhook payload exceeds maximum permitted size of 64KB")));
        }
    }

    // =========================================================================
    // 8. GLOBAL EXCEPTION HANDLER: Type Mismatch & Malformed Payloads
    // =========================================================================
    @Nested
    @DisplayName("GlobalRestExceptionHandler Type Mismatch & Malformed JSON Tests")
    class GlobalExceptionHandlerTests {

        @Test
        @DisplayName("GET /api/v1/shipments/{id} - Non-numeric ID returns 400 with converted type mismatch message")
        void testTypeMismatch_NonNumericId_Returns400() throws Exception {
            mockMvc.perform(get("/api/v1/shipments/notanumber")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message", containsString("cannot be converted to")));
        }

        @Test
        @DisplayName("POST /api/v1/shipments/rate-quote - Malformed JSON body returns 400 Malformed request payload")
        void testMalformedJson_Returns400() throws Exception {
            mockMvc.perform(post("/api/v1/shipments/rate-quote")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"weightKg\": broken_json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Malformed request payload"));
        }
    }
}
