package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.service.DeliveryOtpService;
import com.bazaarhub.backend.delivery.domain.model.*;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryTrackingRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryIntegrationTest {

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
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Autowired
    private SpringDataShipmentRepository shipmentRepository;

    @Autowired
    private SpringDataDeliveryBoyRepository deliveryBoyRepository;

    @Autowired
    private SpringDataDeliveryAssignmentRepository deliveryAssignmentRepository;

    @Autowired
    private SpringDataDeliveryTrackingRepository deliveryTrackingRepository;

    @Autowired
    private DeliveryOtpService deliveryOtpService;

    private String riderToken;
    private String adminToken;
    private String customerToken;

    private UserEntity riderUser;
    private UserEntity adminUser;
    private UserEntity customerUser;

    private SellerEntity seller;
    private OrderEntity order;
    private SellerOrderEntity sellerOrder;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        // Ensure roles exist
        RoleEntity deliveryRole = roleRepository.findByName("ROLE_DELIVERY")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_DELIVERY")
                        .description("Delivery Partner")
                        .build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Admin")
                        .build()));

        // Register Customer
        customerToken = registerAndLogin("customer@delivery.com", "Password@123", "Priya Customer", "+919876543001");
        customerUser = userRepository.findByEmail("customer@delivery.com").orElseThrow();

        // Register Rider (Normal user to onboard)
        riderToken = registerAndLogin("rider@delivery.com", "Password@123", "Rajesh Rider", "+919876543002");
        riderUser = userRepository.findByEmail("rider@delivery.com").orElseThrow();

        // Register Admin and assign ROLE_ADMIN via JDBC
        registerAndLogin("admin@delivery.com", "Password@123", "Super Admin", "+919876543003");
        adminUser = userRepository.findByEmail("admin@delivery.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("admin@delivery.com", "Password@123");

        // Create Seller
        UserEntity sellerUser = userRepository.save(UserEntity.builder()
                .fullName("Seller Vendor")
                .email("seller@delivery.com")
                .phone("+919876543004")
                .passwordHash("hash")
                .build());

        seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("TechGizmo SuperStore")
                .businessEmail("seller@delivery.com")
                .businessPhone("+919876543004")
                .build());

        // Create Address & Order
        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customerUser)
                .recipientName("Priya Customer")
                .phone("+919876543001")
                .addressLine1("B-304, Green Heights")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .latitude(new BigDecimal("19.076090"))
                .longitude(new BigDecimal("72.877426"))
                .build());

        order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-DEL-1001")
                .user(customerUser)
                .shippingAddress(address)
                .recipientName("Priya Customer")
                .recipientPhone("+919876543001")
                .addressLine1("B-304, Green Heights")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .status(OrderStatus.PROCESSING)
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID)
                .subtotal(new BigDecimal("1999.00"))
                .grandTotal(new BigDecimal("1999.00"))
                .build());

        sellerOrder = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-DEL-1001-01")
                .subtotal(new BigDecimal("1999.00"))
                .totalAmount(new BigDecimal("1999.00"))
                .status(SellerOrderStatus.PROCESSING)
                .build());
    }

    @Test
    @DisplayName("Complete Delivery Lifecycle: Onboard -> Verify -> Assign -> Accept -> Pickup -> OutForDelivery -> Complete with OTP")
    void testEndToEndDeliveryWorkflow() throws Exception {
        // 1. Onboard as Delivery Partner
        DeliveryBoyOnboardRequest onboardReq = DeliveryBoyOnboardRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH-02-CD-5678")
                .build();

        MvcResult onboardResult = mockMvc.perform(post("/api/v1/delivery/onboard")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleType").value("BIKE"))
                .andExpect(jsonPath("$.data.vehicleNumber").value("MH-02-CD-5678"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.availabilityStatus").value("OFFLINE"))
                .andReturn();

        JsonNode onboardNode = objectMapper.readTree(onboardResult.getResponse().getContentAsString());
        long deliveryBoyId = onboardNode.path("data").path("id").asLong();

        // Refresh rider token after onboarding to include newly assigned ROLE_DELIVERY
        riderToken = login("rider@delivery.com", "Password@123");

        // 2. Admin Verifies Delivery Partner KYC
        AdminVerifyDeliveryBoyRequest verifyReq = AdminVerifyDeliveryBoyRequest.builder()
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .remarks("License and vehicle verified")
                .build();

        mockMvc.perform(patch("/api/v1/admin/delivery/boys/" + deliveryBoyId + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        // 3. Delivery Partner goes Online (AVAILABLE)
        UpdateAvailabilityRequest availReq = UpdateAvailabilityRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build();

        mockMvc.perform(patch("/api/v1/delivery/availability")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(availReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availabilityStatus").value("AVAILABLE"));

        // 4. Admin Creates Shipment for Seller Order
        CreateShipmentRequest shipmentReq = CreateShipmentRequest.builder()
                .sellerOrderId(sellerOrder.getId())
                .carrierName("BazaarHub Instant")
                .deliveryNotes("Handle with care")
                .build();

        MvcResult shipmentResult = mockMvc.perform(post("/api/v1/admin/delivery/shipments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(shipmentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"))
                .andReturn();

        JsonNode shipmentNode = objectMapper.readTree(shipmentResult.getResponse().getContentAsString());
        long shipmentId = shipmentNode.path("data").path("id").asLong();

        // 5. Admin Assigns Delivery Partner to Shipment
        AssignDeliveryBoyRequest assignReq = AssignDeliveryBoyRequest.builder()
                .shipmentId(shipmentId)
                .deliveryBoyId(deliveryBoyId)
                .build();

        MvcResult assignResult = mockMvc.perform(post("/api/v1/admin/delivery/assignments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.assignmentStatus").value("ASSIGNED"))
                .andReturn();

        JsonNode assignNode = objectMapper.readTree(assignResult.getResponse().getContentAsString());
        long assignmentId = assignNode.path("data").path("id").asLong();

        // 6. Delivery Partner Accepts Assignment
        mockMvc.perform(post("/api/v1/delivery/assignments/" + assignmentId + "/accept")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("ACCEPTED"));

        // 7. Delivery Partner Confirms Pickup from Store
        PickupConfirmationRequest pickupReq = PickupConfirmationRequest.builder()
                .notes("Package collected in pristine condition")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/" + assignmentId + "/pickup")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pickupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("PICKED_UP"))
                .andExpect(jsonPath("$.data.shipmentDetails.status").value("PICKED_UP"));

        // 8. Delivery Partner Marks OUT_FOR_DELIVERY (Triggers OTP generation)
        mockMvc.perform(post("/api/v1/delivery/assignments/" + assignmentId + "/out-for-delivery")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentDetails.status").value("OUT_FOR_DELIVERY"));

        // Generate known valid OTP for the test verification
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);

        // 9. Delivery Partner Submits Live GPS Location
        UpdateLocationRequest locReq = UpdateLocationRequest.builder()
                .shipmentId(shipmentId)
                .latitude(new BigDecimal("19.076090"))
                .longitude(new BigDecimal("72.877426"))
                .build();

        mockMvc.perform(post("/api/v1/delivery/tracking/location")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latitude").value(19.07609))
                .andExpect(jsonPath("$.data.longitude").value(72.877426));

        // 10. Customer Tracks Live Location
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipmentId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.latitude").value(19.07609));

        // 11. Completing with Wrong OTP Fails
        CompleteDeliveryRequest wrongOtpReq = CompleteDeliveryRequest.builder()
                .otp("000000")
                .notes("Delivery attempt")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/" + assignmentId + "/complete")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // 12. Completing with Valid OTP Succeeds
        CompleteDeliveryRequest validOtpReq = CompleteDeliveryRequest.builder()
                .otp(plainOtp)
                .notes("Package delivered to recipient doorstep")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/" + assignmentId + "/complete")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.shipmentDetails.status").value("DELIVERED"));

        // Verify database state
        assertEquals(ShipmentStatus.DELIVERED, shipmentRepository.findById(shipmentId).orElseThrow().getStatus());
        assertEquals(DeliveryAssignmentStatus.COMPLETED, deliveryAssignmentRepository.findById(assignmentId).orElseThrow().getAssignmentStatus());
        assertEquals(SellerOrderStatus.DELIVERED, sellerOrderRepository.findById(sellerOrder.getId()).orElseThrow().getStatus());
        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    private String registerAndLogin(String email, String password, String name, String phone) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName(name)
                .email(email)
                .phone(phone)
                .password(password)
                .confirmPassword(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .identifier(email)
                .password(password)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
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

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM delivery_tracking");
        jdbcTemplate.execute("DELETE FROM delivery_assignments");
        jdbcTemplate.execute("DELETE FROM shipments");
        jdbcTemplate.execute("DELETE FROM delivery_boys");
        jdbcTemplate.execute("DELETE FROM order_status_history");
        jdbcTemplate.execute("DELETE FROM order_items");
        jdbcTemplate.execute("DELETE FROM seller_orders");
        jdbcTemplate.execute("DELETE FROM orders");
        jdbcTemplate.execute("DELETE FROM cart_items");
        jdbcTemplate.execute("DELETE FROM carts");
        jdbcTemplate.execute("DELETE FROM wishlist_items");
        jdbcTemplate.execute("DELETE FROM wishlists");
        jdbcTemplate.execute("DELETE FROM product_variants");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM brands");
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM stores");
        jdbcTemplate.execute("DELETE FROM sellers");
        jdbcTemplate.execute("DELETE FROM user_devices");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
