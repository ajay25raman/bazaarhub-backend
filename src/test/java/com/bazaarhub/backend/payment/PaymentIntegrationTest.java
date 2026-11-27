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
import com.bazaarhub.backend.payment.application.dto.request.CreateRazorpayOrderRequest;
import com.bazaarhub.backend.payment.application.dto.request.CreateRefundRequest;
import com.bazaarhub.backend.payment.application.dto.request.SelectCodPaymentRequest;
import com.bazaarhub.backend.payment.application.dto.request.VerifyRazorpayPaymentRequest;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.infrastructure.gateway.PaymentGateway;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayOrderResponse;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayPaymentDetails;
import com.bazaarhub.backend.payment.infrastructure.gateway.model.RazorpayRefundResponse;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentTransactionRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataRefundRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentIntegrationTest {

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

    private String customer1Token;
    private String customer2Token;
    private String adminToken;

    private UserEntity customer1User;
    private UserEntity customer2User;
    private UserEntity adminUser;

    private OrderEntity order1;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database tables in reverse dependency order
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
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM customer_profiles");
        jdbcTemplate.execute("DELETE FROM user_devices");
        userRepository.deleteAll();

        // Permissions
        PermissionEntity paymentCreate = ensurePermission("PAYMENT_CREATE", "Create payment");
        PermissionEntity paymentRead = ensurePermission("PAYMENT_READ", "Read payment");
        PermissionEntity paymentManage = ensurePermission("PAYMENT_MANAGE", "Manage payment");
        PermissionEntity refundManage = ensurePermission("REFUND_MANAGE", "Manage refunds");
        PermissionEntity orderCreate = ensurePermission("ORDER_CREATE", "Create order");
        PermissionEntity orderRead = ensurePermission("ORDER_READ", "Read order");

        // Roles
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .permissions(Set.of(paymentCreate, paymentRead, orderCreate, orderRead))
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Admin")
                        .permissions(Set.of(paymentCreate, paymentRead, paymentManage, refundManage, orderRead))
                        .build()));

        // Register Customer 1
        customer1Token = registerAndLogin("alice@payment.com", "Password@123", "Alice Payment", "+919876520001");
        customer1User = userRepository.findByEmail("alice@payment.com").orElseThrow();

        // Register Customer 2
        customer2Token = registerAndLogin("bob@payment.com", "Password@123", "Bob Payment", "+919876520002");
        customer2User = userRepository.findByEmail("bob@payment.com").orElseThrow();

        // Register Admin
        registerAndLogin("admin@payment.com", "Password@123", "Admin Chief", "+919876520003");
        adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin@payment.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin@payment.com", "Password@123");

        // Address
        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customer1User)
                .addressType("HOME")
                .recipientName("Alice Payment")
                .phone("+919876520001")
                .addressLine1("123 Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .country("India")
                .isDefault(true)
                .build());

        // Create base order for Customer 1
        order1 = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-PAY-TEST-001")
                .user(customer1User)
                .shippingAddress(address)
                .recipientName("Alice Payment")
                .recipientPhone("+919876520001")
                .addressLine1("123 Indiranagar")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .country("India")
                .subtotal(new BigDecimal("1499.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("1499.00"))
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING)
                .status(OrderStatus.PENDING)
                .build());

        // Default mock gateway behavior
        when(paymentGateway.createRazorpayOrder(anyString(), any(BigDecimal.class), anyString(), anyMap()))
                .thenReturn(RazorpayOrderResponse.builder()
                        .id("order_MOCK_123")
                        .amount(149900L)
                        .amountDecimal(new BigDecimal("1499.00"))
                        .currency("INR")
                        .receipt("ORD-PAY-TEST-001")
                        .status("created")
                        .build());

        when(paymentGateway.verifyPaymentSignature(anyString(), anyString(), eq("valid_sig")))
                .thenReturn(true);
        when(paymentGateway.verifyPaymentSignature(anyString(), anyString(), eq("bad_sig")))
                .thenReturn(false);

        when(paymentGateway.verifyWebhookSignature(anyString(), eq("valid_webhook_sig")))
                .thenReturn(true);
        when(paymentGateway.verifyWebhookSignature(anyString(), eq("bad_webhook_sig")))
                .thenReturn(false);

        when(paymentGateway.fetchPaymentDetails("pay_MOCK_789"))
                .thenReturn(RazorpayPaymentDetails.builder()
                        .id("pay_MOCK_789")
                        .method("upi")
                        .status("captured")
                        .amount(149900L)
                        .currency("INR")
                        .build());

        when(paymentGateway.initiateRefund(eq("pay_MOCK_789"), any(BigDecimal.class), anyString(), anyString()))
                .thenReturn(RazorpayRefundResponse.builder()
                        .id("rfnd_MOCK_456")
                        .paymentId("pay_MOCK_789")
                        .amount(50000L)
                        .amountDecimal(new BigDecimal("500.00"))
                        .currency("INR")
                        .status("processed")
                        .build());
    }

    private PermissionEntity ensurePermission(String name, String desc) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name(name).description(desc).build()));
    }

    private String registerAndLogin(String email, String password, String fullName, String phone) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .password(password)
                .confirmPassword(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .identifier(email)
                .password(password)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    // =========================================================================
    // 1. RAZORPAY ORDER INITIATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Scenario 1: Customer initiates Razorpay checkout order successfully")
    void testCreateRazorpayOrder_Success() throws Exception {
        CreateRazorpayOrderRequest request = new CreateRazorpayOrderRequest(order1.getId());

        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(order1.getId()))
                .andExpect(jsonPath("$.data.orderNumber").value(order1.getOrderNumber()))
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_MOCK_123"))
                .andExpect(jsonPath("$.data.amount").value(1499.00))
                .andExpect(jsonPath("$.data.amountInPaise").value(149900))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertEquals(1, paymentRepository.findAll().size());
        assertEquals(1, transactionRepository.findAll().size());
    }

    @Test
    @DisplayName("Scenario 2: Customer receives idempotent response when re-initiating pending checkout")
    void testCreateRazorpayOrder_Idempotent() throws Exception {
        CreateRazorpayOrderRequest request = new CreateRazorpayOrderRequest(order1.getId());

        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Re-call endpoint
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_MOCK_123"));

        // Only 1 payment record should be created
        assertEquals(1, paymentRepository.findAll().size());
    }

    @Test
    @DisplayName("Scenario 3: Non-owner customer attempting to pay receives 403 Forbidden")
    void testCreateRazorpayOrder_ForbiddenForNonOwner() throws Exception {
        CreateRazorpayOrderRequest request = new CreateRazorpayOrderRequest(order1.getId());

        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer2Token) // Customer 2 token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. RAZORPAY PAYMENT VERIFICATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Scenario 4: Valid Razorpay signature verifies payment, captures payment and marks order CONFIRMED/PAID")
    void testVerifyRazorpayPayment_Success() throws Exception {
        // Step 1: Initiate order
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRazorpayOrderRequest(order1.getId()))))
                .andExpect(status().isCreated());

        // Step 2: Verify payment with valid signature
        VerifyRazorpayPaymentRequest verifyRequest = VerifyRazorpayPaymentRequest.builder()
                .orderId(order1.getId())
                .razorpayOrderId("order_MOCK_123")
                .razorpayPaymentId("pay_MOCK_789")
                .razorpaySignature("valid_sig")
                .build();

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CAPTURED"))
                .andExpect(jsonPath("$.data.gatewayPaymentId").value("pay_MOCK_789"))
                .andExpect(jsonPath("$.data.paymentMethod").value("upi"));

        // Verify order in database
        OrderEntity updatedOrder = orderRepository.findById(order1.getId()).orElseThrow();
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, updatedOrder.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
    }

    @Test
    @DisplayName("Scenario 5: Invalid Razorpay signature is rejected with 400 Bad Request")
    void testVerifyRazorpayPayment_InvalidSignature() throws Exception {
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRazorpayOrderRequest(order1.getId()))))
                .andExpect(status().isCreated());

        VerifyRazorpayPaymentRequest verifyRequest = VerifyRazorpayPaymentRequest.builder()
                .orderId(order1.getId())
                .razorpayOrderId("order_MOCK_123")
                .razorpayPaymentId("pay_MOCK_789")
                .razorpaySignature("bad_sig")
                .build();

        mockMvc.perform(post("/api/v1/payments/razorpay/verify")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Cryptographic verification failed")));
    }

    // =========================================================================
    // 3. CASH ON DELIVERY (COD) TESTS
    // =========================================================================

    @Test
    @DisplayName("Scenario 6: Customer selects Cash on Delivery (COD) and order transitions to CONFIRMED")
    void testSelectCodPayment_Success() throws Exception {
        SelectCodPaymentRequest request = new SelectCodPaymentRequest(order1.getId());

        mockMvc.perform(post("/api/v1/payments/cod/select")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gateway").value("COD"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        OrderEntity updatedOrder = orderRepository.findById(order1.getId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus());
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING, updatedOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Scenario 7: Admin marks COD payment as collected and reconciles order to PAID")
    void testCollectCodPayment_Admin_Success() throws Exception {
        // Step 1: Customer selects COD
        MvcResult codResult = mockMvc.perform(post("/api/v1/payments/cod/select")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SelectCodPaymentRequest(order1.getId()))))
                .andExpect(status().isOk())
                .andReturn();

        Long paymentId = objectMapper.readTree(codResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Step 2: Admin collects COD
        CollectCodPaymentRequest collectReq = new CollectCodPaymentRequest(new BigDecimal("1499.00"), "Cash delivered to hub");
        mockMvc.perform(post("/api/v1/admin/payments/" + paymentId + "/cod-collect")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(collectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CAPTURED"));

        OrderEntity updatedOrder = orderRepository.findById(order1.getId()).orElseThrow();
        assertEquals(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID, updatedOrder.getPaymentStatus());
    }

    @Test
    @DisplayName("Scenario 8: Non-admin customer attempting to collect COD receives 403 Forbidden")
    void testCollectCodPayment_CustomerForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payments/999/cod-collect")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. RAZORPAY WEBHOOK TESTS (PUBLIC AUTH-FREE ENDPOINT)
    // =========================================================================

    @Test
    @DisplayName("Scenario 9: Razorpay payment.captured webhook captures payment without JWT token")
    void testWebhook_PaymentCaptured_Success() throws Exception {
        // Initiate order first
        mockMvc.perform(post("/api/v1/payments/razorpay/create-order")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRazorpayOrderRequest(order1.getId()))))
                .andExpect(status().isCreated());

        String webhookBody = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_MOCK_789",
                        "order_id": "order_MOCK_123",
                        "amount": 149900,
                        "currency": "INR",
                        "status": "captured",
                        "method": "upi"
                      }
                    }
                  }
                }
                """;

        // Call public webhook endpoint without Authorization header
        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "valid_webhook_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.event").value("payment.captured"))
                .andExpect(jsonPath("$.data.success").value(true));

        PaymentEntity payment = paymentRepository.findByGatewayOrderId("order_MOCK_123").orElseThrow();
        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
        assertEquals("pay_MOCK_789", payment.getGatewayPaymentId());
    }

    @Test
    @DisplayName("Scenario 10: Razorpay webhook with invalid signature returns 400 Bad Request")
    void testWebhook_InvalidSignature_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "bad_webhook_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"payment.captured\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 5. ADMIN REFUND TESTS
    // =========================================================================

    @Test
    @DisplayName("Scenario 11: Admin executes partial refund on captured payment")
    void testAdminRefund_Success() throws Exception {
        // Setup captured payment
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .order(order1)
                .paymentReference("PAY-CAPTURED-001")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_MOCK_123")
                .gatewayPaymentId("pay_MOCK_789")
                .amount(new BigDecimal("1499.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .paidAt(java.time.Instant.now())
                .createdBy(customer1User)
                .updatedBy(customer1User)
                .build());

        CreateRefundRequest refundRequest = new CreateRefundRequest(new BigDecimal("500.00"), "Customer requested refund for item defect");

        mockMvc.perform(post("/api/v1/admin/payments/" + payment.getId() + "/refunds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.refundAmount").value(500.00))
                .andExpect(jsonPath("$.data.gatewayRefundId").value("rfnd_MOCK_456"));

        PaymentEntity updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, updatedPayment.getStatus());
    }

    @Test
    @DisplayName("Scenario 12: Admin refund exceeding available balance is rejected with 400 Bad Request")
    void testAdminRefund_ExceedingBalance_Returns400() throws Exception {
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .order(order1)
                .paymentReference("PAY-CAPTURED-002")
                .gateway("RAZORPAY")
                .gatewayOrderId("order_MOCK_123")
                .gatewayPaymentId("pay_MOCK_789")
                .amount(new BigDecimal("500.00"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .build());

        // Attempt to refund ₹600 on ₹500 payment
        CreateRefundRequest refundRequest = new CreateRefundRequest(new BigDecimal("600.00"), "Exceeding refund");

        mockMvc.perform(post("/api/v1/admin/payments/" + payment.getId() + "/refunds")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("exceeds the maximum available refundable balance")));
    }

    // =========================================================================
    // 6. PAYMENT QUERY TESTS & AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("Scenario 13: Customer can view payment by ID, while unauthorized customer receives 403")
    void testGetPaymentById_OwnershipEnforced() throws Exception {
        PaymentEntity payment = paymentRepository.save(PaymentEntity.builder()
                .order(order1)
                .paymentReference("PAY-VIEW-001")
                .gateway("RAZORPAY")
                .amount(new BigDecimal("1499.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build());

        // Owner customer 1 gets payment
        mockMvc.perform(get("/api/v1/payments/" + payment.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-VIEW-001"));

        // Customer 2 receives 403
        mockMvc.perform(get("/api/v1/payments/" + payment.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());

        // Admin gets payment
        mockMvc.perform(get("/api/v1/payments/" + payment.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentReference").value("PAY-VIEW-001"));
    }

    @Test
    @DisplayName("Scenario 14: Customer lists their payments and Admin lists all payments")
    void testListPayments_CustomerAndAdmin() throws Exception {
        paymentRepository.save(PaymentEntity.builder()
                .order(order1)
                .paymentReference("PAY-LIST-001")
                .gateway("RAZORPAY")
                .amount(new BigDecimal("1499.00"))
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build());

        // Customer lists payments
        mockMvc.perform(get("/api/v1/payments")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Admin lists all payments
        mockMvc.perform(get("/api/v1/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM refunds");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM order_status_history");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM seller_orders");
        jdbcTemplate.execute("DELETE FROM orders");
    }
}
