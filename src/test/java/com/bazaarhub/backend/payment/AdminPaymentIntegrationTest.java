package com.bazaarhub.backend.payment;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.payment.application.dto.request.CollectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.RefundStatus;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dedicated Integration Test suite for AdminPaymentController.
 * Validates Cash on Delivery collection, refund issuance, payment & refund ledger listings,
 * refund detail lookup, and RBAC security boundaries.
 * Stage 5 REM-TEST-01 verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataPermissionRepository permissionRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;

    @Autowired
    private SpringDataPaymentTransactionRepository transactionRepository;

    @Autowired
    private SpringDataRefundRepository refundRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    private String adminToken;
    private String customerToken;

    private UserEntity customerUser;
    private UserEntity adminUser;

    private OrderEntity order;
    private PaymentEntity codPayment;
    private PaymentEntity razorpayPayment;
    private RefundEntity existingRefund;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database in reverse dependency order
        jdbcTemplate.execute("DELETE FROM delivery_tracking");
        jdbcTemplate.execute("DELETE FROM delivery_assignments");
        jdbcTemplate.execute("DELETE FROM shipments");
        jdbcTemplate.execute("DELETE FROM delivery_boys");
        jdbcTemplate.execute("DELETE FROM refunds");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM order_status_history");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM seller_orders");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM cart_items");
        jdbcTemplate.execute("DELETE FROM carts");
        jdbcTemplate.execute("DELETE FROM wishlist_items");
        jdbcTemplate.execute("DELETE FROM wishlists");
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM customer_profiles");
        jdbcTemplate.execute("DELETE FROM inventory_movements");
        jdbcTemplate.execute("DELETE FROM stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventories");
        jdbcTemplate.execute("DELETE FROM product_variants");
        jdbcTemplate.execute("DELETE FROM product_images");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM seller_bank_accounts");
        jdbcTemplate.execute("DELETE FROM seller_documents");
        jdbcTemplate.execute("DELETE FROM stores");
        jdbcTemplate.execute("DELETE FROM sellers");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM user_devices");
        jdbcTemplate.execute("DELETE FROM users");

        // 1. Permissions & Roles
        PermissionEntity paymentManage = permissionRepository.findByName("PAYMENT_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("PAYMENT_MANAGE").description("Manage payments").build()));
        PermissionEntity refundManage = permissionRepository.findByName("REFUND_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("REFUND_MANAGE").description("Manage refunds").build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Admin")
                        .permissions(Set.of(paymentManage, refundManage))
                        .build()));

        // 2. Admin User
        registerAndLogin("admin.payment@bazaarhub.com", "AdminPass123!", "Payment Admin", "+919400000001");
        adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin.payment@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin.payment@bazaarhub.com", "AdminPass123!");

        // 3. Customer User
        customerToken = registerAndLogin("customer.payment@bazaarhub.com", "CustomerPass123!", "Payment Customer", "+919400000002");
        customerUser = userRepository.findByIdentifierWithRolesAndPermissions("customer.payment@bazaarhub.com").orElseThrow();

        // 4. Address & Order
        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customerUser)
                .recipientName("Payment Customer")
                .phone("+919400000002")
                .addressLine1("100 Nariman Point")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400021")
                .country("India")
                .build());

        order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-ADM-PAY-001")
                .user(customerUser)
                .shippingAddress(address)
                .recipientName("Payment Customer")
                .recipientPhone("+919400000002")
                .addressLine1("100 Nariman Point")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400021")
                .country("India")
                .subtotal(new BigDecimal("2000.00"))
                .deliveryFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("2000.00"))
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING)
                .status(OrderStatus.CONFIRMED)
                .build());

        // 5. COD Payment (Pending)
        codPayment = paymentRepository.save(PaymentEntity.builder()
                .order(order)
                .paymentReference("PAY-COD-ADM-001")
                .gateway("COD")
                .paymentMethod("CASH")
                .amount(new BigDecimal("2000.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .createdBy(customerUser)
                .updatedBy(customerUser)
                .build());

        // 6. Razorpay Payment (Captured)
        razorpayPayment = paymentRepository.save(PaymentEntity.builder()
                .order(order)
                .paymentReference("PAY-RP-ADM-002")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_ADM_123")
                .gatewayPaymentId("pay_ADM_789")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .paidAt(Instant.now())
                .createdBy(customerUser)
                .updatedBy(customerUser)
                .build());

        // 7. Seed Refund Record
        existingRefund = refundRepository.save(RefundEntity.builder()
                .payment(razorpayPayment)
                .order(order)
                .requestedBy(adminUser)
                .refundReference("RFND-EXISTING-001")
                .gatewayRefundId("rfnd_EXISTING_123")
                .refundAmount(new BigDecimal("300.00"))
                .status(RefundStatus.COMPLETED)
                .reason("Damaged package reimbursement")
                .processedBy(adminUser)
                .processedAt(Instant.now())
                .build());

        // 8. Gateway Mock setup
        when(paymentGateway.initiateRefund(anyString(), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(RazorpayRefundResponse.builder()
                        .id("rfnd_TEST_999")
                        .paymentId("pay_ADM_789")
                        .amount(50000L)
                        .amountDecimal(new BigDecimal("500.00"))
                        .currency("INR")
                        .status("processed")
                        .build());
    }

    private String registerAndLogin(String email, String password, String fullName, String phone) throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(password)
                .confirmPassword(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    private String login(String identifier, String password) throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .identifier(identifier)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin can collect Cash on Delivery payment and reconcile order status to PAID")
    void collectCodPayment_success() throws Exception {
        CollectCodPaymentRequest request = new CollectCodPaymentRequest(new BigDecimal("2000.00"), "Cash verified at hub");

        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/cod-collect", codPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CAPTURED"));

        // Reconcile order payment status
        OrderEntity updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, updatedOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Admin collecting non-COD payment fails with 400 Bad Request, and collecting COD is idempotent")
    void collectCodPayment_nonCodPayment_fails() throws Exception {
        // Collecting non-COD payment throws 400 Bad Request
        CollectCodPaymentRequest request = new CollectCodPaymentRequest(new BigDecimal("1500.00"), "Invalid attempt");
        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/cod-collect", razorpayPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Re-collecting already captured COD payment is idempotent and returns 200 OK
        CollectCodPaymentRequest codRequest = new CollectCodPaymentRequest(new BigDecimal("2000.00"), "First collection");
        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/cod-collect", codPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/cod-collect", codPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(codRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CAPTURED"));
    }

    @Test
    @DisplayName("Admin can initiate refund against captured payment")
    void createRefund_success() throws Exception {
        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("500.00"), "Partial refund for missing item");

        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/refunds", razorpayPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.refundAmount").value(500.00))
                .andExpect(jsonPath("$.data.gatewayRefundId").value("rfnd_TEST_999"));

        // Verify payment is updated to PARTIALLY_REFUNDED
        PaymentEntity updatedPayment = paymentRepository.findById(razorpayPayment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, updatedPayment.getStatus());
    }

    @Test
    @DisplayName("Admin refund exceeding available balance is rejected with 400 Bad Request")
    void createRefund_exceedingBalance_fails() throws Exception {
        // ₹1500 payment with existing ₹300 refund leaves max ₹1200 refundable
        CreateRefundRequest request = new CreateRefundRequest(new BigDecimal("1300.00"), "Exceeding available balance");

        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/refunds", razorpayPayment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Admin can list all platform payments with pagination")
    void listAllPayments_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Admin can list all platform refunds with pagination")
    void listAllRefunds_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].refundReference").value("RFND-EXISTING-001"));
    }

    @Test
    @DisplayName("Admin can retrieve refund details by ID")
    void getRefundById_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/{refundId}", existingRefund.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(existingRefund.getId()))
                .andExpect(jsonPath("$.data.refundAmount").value(300.00))
                .andExpect(jsonPath("$.data.reason").value("Damaged package reimbursement"));
    }

    @Test
    @DisplayName("Non-existent refund ID returns 404 Not Found")
    void getRefundById_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Customer role cannot access Admin Payment endpoints -> 403 Forbidden")
    void customerAccess_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/refunds")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/payments/{paymentId}/cod-collect", codPayment.getId())
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to Admin Payment endpoints returns 401 Unauthorized")
    void unauthenticatedAccess_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/refunds"))
                .andExpect(status().isUnauthorized());
    }
}
