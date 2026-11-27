package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.service.DeliveryOtpService;
import com.bazaarhub.backend.delivery.domain.model.*;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
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
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Targeted Integration Test suite for DeliveryBoyController and Admin Delivery Operations.
 * Covers delivery partner onboarding, profile management, availability toggle, assignment
 * lifecycle (accept, pickup, out-for-delivery, OTP complete, fail), GPS tracking, proof upload,
 * and RBAC protection.
 * Stage 5 REM-TEST-01 verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryBoyControllerIntegrationTest {

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
    private Long riderUserId;
    private Long riderDeliveryBoyId;

    private String secondRiderToken;
    private Long secondRiderUserId;

    private String adminToken;
    private String customerToken;

    private Long shipmentId;
    private Long assignmentId;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM delivery_tracking");
        jdbcTemplate.execute("DELETE FROM delivery_assignments");
        jdbcTemplate.execute("DELETE FROM shipments");
        jdbcTemplate.execute("DELETE FROM delivery_boys");
        jdbcTemplate.execute("DELETE FROM payment_transactions");
        jdbcTemplate.execute("DELETE FROM refunds");
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

        // 1. Ensure Roles
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));

        RoleEntity deliveryRole = roleRepository.findByName("ROLE_DELIVERY")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_DELIVERY").description("Delivery Partner").build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // 2. Admin
        registerAndLogin("Delivery Admin", "admin.delivery@bazaarhub.com", "+919300000001", "AdminPass123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin.delivery@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin.delivery@bazaarhub.com", "AdminPass123!");

        // 3. Customer
        customerToken = registerAndLogin("Customer Dave", "customer.delivery@bazaarhub.com", "+919300000002", "CustomerPass123!");
        UserEntity customerUser = userRepository.findByIdentifierWithRolesAndPermissions("customer.delivery@bazaarhub.com").orElseThrow();

        // 4. Primary Delivery Rider
        registerAndLogin("Rider Rob", "rider.delivery@bazaarhub.com", "+919300000003", "RiderPass123!");
        UserEntity riderUser = userRepository.findByIdentifierWithRolesAndPermissions("rider.delivery@bazaarhub.com").orElseThrow();
        riderUser.getRoles().add(deliveryRole);
        userRepository.save(riderUser);
        riderUserId = riderUser.getId();
        riderToken = login("rider.delivery@bazaarhub.com", "RiderPass123!");

        DeliveryBoyEntity riderEntity = deliveryBoyRepository.save(DeliveryBoyEntity.builder()
                .user(riderUser)
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH02-AB-1234")
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build());
        riderDeliveryBoyId = riderEntity.getId();

        // 5. Candidate Rider (for onboard test)
        registerAndLogin("Candidate Rider", "new.rider@bazaarhub.com", "+919300000004", "NewRider123!");
        secondRiderToken = login("new.rider@bazaarhub.com", "NewRider123!");
        secondRiderUserId = userRepository.findByIdentifierWithRolesAndPermissions("new.rider@bazaarhub.com").orElseThrow().getId();

        // 6. Seed Seller, Order & Shipment
        registerAndLogin("Seller Sam", "seller.dispatch@bazaarhub.com", "+919300000005", "SellerPass123!");
        UserEntity sellerUser = userRepository.findByIdentifierWithRolesAndPermissions("seller.dispatch@bazaarhub.com").orElseThrow();

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Fast Dispatch Hub")
                .businessEmail("dispatch@fastdispatch.com")
                .businessPhone("+919300000009")
                .build());

        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customerUser)
                .recipientName("Customer Dave")
                .phone("+919300000002")
                .addressLine1("Apartment 12, Sunrise Tower")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .latitude(new BigDecimal("18.9220"))
                .longitude(new BigDecimal("72.8347"))
                .build());

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-DEL-101")
                .user(customerUser)
                .shippingAddress(address)
                .recipientName("Customer Dave")
                .recipientPhone("+919300000002")
                .addressLine1("Apartment 12, Sunrise Tower")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .status(OrderStatus.PROCESSING)
                .subtotal(new BigDecimal("1200.00"))
                .grandTotal(new BigDecimal("1200.00"))
                .build());

        SellerOrderEntity sellerOrder = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-DEL-101-A")
                .subtotal(new BigDecimal("1200.00"))
                .totalAmount(new BigDecimal("1200.00"))
                .status(SellerOrderStatus.PROCESSING)
                .build());

        ShipmentEntity shipment = shipmentRepository.save(ShipmentEntity.builder()
                .sellerOrder(sellerOrder)
                .shipmentNumber("SHP-DEL-101")
                .carrierName("BazaarHub Logistics")
                .trackingNumber("TRK-DEL-101")
                .status(ShipmentStatus.READY_FOR_PICKUP)
                .build());
        shipmentId = shipment.getId();

        DeliveryAssignmentEntity assignment = deliveryAssignmentRepository.save(DeliveryAssignmentEntity.builder()
                .shipment(shipment)
                .deliveryBoy(riderEntity)
                .assignmentStatus(DeliveryAssignmentStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build());
        assignmentId = assignment.getId();
    }

    private String registerAndLogin(String name, String email, String phone, String password) throws Exception {
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName(name)
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
    @DisplayName("Candidate user can onboard as delivery partner")
    void onboard_success() throws Exception {
        DeliveryBoyOnboardRequest request = DeliveryBoyOnboardRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH01-CD-5678")
                .build();

        mockMvc.perform(post("/api/v1/delivery/onboard")
                        .header("Authorization", "Bearer " + secondRiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("MH01-CD-5678"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Delivery partner can view and update their profile")
    void profile_viewAndUpdate() throws Exception {
        // View profile
        mockMvc.perform(get("/api/v1/delivery/profile")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("MH02-AB-1234"));

        // Update profile
        DeliveryBoyUpdateRequest updateReq = DeliveryBoyUpdateRequest.builder()
                .vehicleType(VehicleType.SCOOTER)
                .vehicleNumber("MH02-EV-9999")
                .build();

        mockMvc.perform(put("/api/v1/delivery/profile")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleNumber").value("MH02-EV-9999"));
    }

    @Test
    @DisplayName("Delivery partner can toggle availability status")
    void updateAvailability_success() throws Exception {
        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.OFFLINE)
                .build();

        mockMvc.perform(patch("/api/v1/delivery/availability")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availabilityStatus").value("OFFLINE"));
    }

    @Test
    @DisplayName("Admin can verify delivery partner KYC documentation")
    void adminVerifyDeliveryBoy_success() throws Exception {
        AdminVerifyDeliveryBoyRequest request = AdminVerifyDeliveryBoyRequest.builder()
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .remarks("All identity documents verified against government database")
                .build();

        mockMvc.perform(patch("/api/v1/admin/delivery/boys/{id}/verify", riderDeliveryBoyId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Delivery partner can list their assignments")
    void getAssignments_success() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/assignments")
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(assignmentId));
    }

    @Test
    @DisplayName("Delivery partner can view assignment details by ID")
    void getAssignmentById_success() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/assignments/{id}", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(assignmentId));
    }

    @Test
    @DisplayName("Delivery partner workflow: Accept -> Pickup -> Out for Delivery -> Complete with OTP")
    void completeDeliveryWorkflow_success() throws Exception {
        // 1. Accept
        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/accept", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("ACCEPTED"));

        // 2. Pickup
        PickupConfirmationRequest pickupReq = PickupConfirmationRequest.builder()
                .notes("Picked up packaged parcel from warehouse shelf A1")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/pickup", assignmentId)
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pickupReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("PICKED_UP"));

        // 3. Mark Out for Delivery (generates OTP)
        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/out-for-delivery", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk());

        // Generate known valid OTP for verification
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipmentId);

        // 4. Complete Delivery
        CompleteDeliveryRequest completeReq = CompleteDeliveryRequest.builder()
                .otp(plainOtp)
                .notes("Delivered to customer directly at entrance")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/complete", assignmentId)
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.shipmentDetails.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("Delivery partner workflow: Accept -> Pickup -> Out for Delivery -> Fail Delivery with reason")
    void failDeliveryWorkflow_success() throws Exception {
        // 1. Accept
        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/accept", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk());

        // 2. Pickup
        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/pickup", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk());

        // 3. Mark Out for Delivery
        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/out-for-delivery", assignmentId)
                        .header("Authorization", "Bearer " + riderToken))
                .andExpect(status().isOk());

        // 4. Report Delivery Failure
        FailedDeliveryRequest failReq = FailedDeliveryRequest.builder()
                .failureReason(DeliveryFailureReason.CUSTOMER_UNAVAILABLE)
                .notes("Customer phone unanswered after 3 attempts at door")
                .build();

        mockMvc.perform(post("/api/v1/delivery/assignments/{id}/fail", assignmentId)
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentDetails.status").value("FAILED"));
    }

    @Test
    @DisplayName("Delivery partner can record live GPS location")
    void recordLocation_success() throws Exception {
        // Shipment must be in transit to record location
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId).orElseThrow();
        shipment.setStatus(ShipmentStatus.IN_TRANSIT);
        shipmentRepository.save(shipment);

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
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId));
    }

    @Test
    @DisplayName("Delivery partner can upload delivery proof photo")
    void uploadProof_success() throws Exception {
        DeliveryProofUploadRequest request = DeliveryProofUploadRequest.builder()
                .image("https://example.com/mock-doorstep-photo.jpg")
                .folder("bazaarhub/delivery/proofs")
                .build();

        mockMvc.perform(post("/api/v1/delivery/proof/upload")
                        .header("Authorization", "Bearer " + riderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("https://example.com/mock-doorstep-photo.jpg"));
    }

    @Test
    @DisplayName("Customer role cannot access delivery assignments -> 403 Forbidden")
    void customerAccess_forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/assignments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to delivery profile returns 401 Unauthorized")
    void unauthenticatedAccess_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/profile"))
                .andExpect(status().isUnauthorized());
    }
}
