package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.delivery.application.dto.request.AdminVerifyDeliveryBoyRequest;
import com.bazaarhub.backend.delivery.application.dto.request.DeliveryBoyOnboardRequest;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryTrackingEntity;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Targeted Integration Test suite validating Broken Object Level Authorization (BOLA / IDOR)
 * defense across Delivery Tracking and Route Estimation endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CustomerDeliveryTrackingSecurityIntegrationTest {

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

    private String ownerCustomerToken;
    private String attackerCustomerToken;
    private String assignedRiderToken;
    private String unassignedRiderToken;
    private String adminToken;

    private UserEntity ownerCustomer;
    private UserEntity attackerCustomer;
    private UserEntity assignedRiderUser;
    private UserEntity unassignedRiderUser;
    private UserEntity adminUser;

    private DeliveryBoyEntity assignedDeliveryBoy;
    private DeliveryBoyEntity unassignedDeliveryBoy;

    private ShipmentEntity shipment1;
    private ShipmentEntity shipment2;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        // 1. Ensure Roles Exist
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .build()));

        RoleEntity deliveryRole = roleRepository.findByName("ROLE_DELIVERY")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_DELIVERY")
                        .description("Delivery Partner")
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator")
                        .build()));

        // 2. Register Owner Customer & Attacker Customer
        ownerCustomerToken = registerAndLogin("owner@customer.com", "Password@123", "Owner Customer", "+919876543101");
        ownerCustomer = userRepository.findByEmail("owner@customer.com").orElseThrow();

        attackerCustomerToken = registerAndLogin("attacker@customer.com", "Password@123", "Attacker Customer", "+919876543102");
        attackerCustomer = userRepository.findByEmail("attacker@customer.com").orElseThrow();

        // 3. Register Admin User
        registerAndLogin("admin@bazaarhub.com", "Password@123", "Admin User", "+919876543103");
        adminUser = userRepository.findByEmail("admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("admin@bazaarhub.com", "Password@123");

        // 4. Onboard and Verify Assigned Delivery Partner
        assignedRiderToken = registerAndLogin("assigned@rider.com", "Password@123", "Assigned Rider", "+919876543104");
        assignedRiderUser = userRepository.findByEmail("assigned@rider.com").orElseThrow();
        long assignedRiderDbId = onboardAndVerifyRider(assignedRiderToken, adminToken, "MH-01-AB-1111");
        assignedRiderToken = login("assigned@rider.com", "Password@123");
        assignedDeliveryBoy = deliveryBoyRepository.findById(assignedRiderDbId).orElseThrow();

        // 5. Onboard and Verify Unassigned Delivery Partner
        unassignedRiderToken = registerAndLogin("unassigned@rider.com", "Password@123", "Unassigned Rider", "+919876543105");
        unassignedRiderUser = userRepository.findByEmail("unassigned@rider.com").orElseThrow();
        long unassignedRiderDbId = onboardAndVerifyRider(unassignedRiderToken, adminToken, "MH-01-AB-2222");
        unassignedRiderToken = login("unassigned@rider.com", "Password@123");
        unassignedDeliveryBoy = deliveryBoyRepository.findById(unassignedRiderDbId).orElseThrow();

        // 6. Create Seller & Address
        UserEntity sellerUser = userRepository.save(UserEntity.builder()
                .fullName("Seller Vendor")
                .email("vendor@bazaarhub.com")
                .phone("+919876543106")
                .passwordHash("hash")
                .build());

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Prime Retail Store")
                .businessEmail("vendor@bazaarhub.com")
                .businessPhone("+919876543106")
                .build());

        AddressEntity address1 = addressRepository.save(AddressEntity.builder()
                .user(ownerCustomer)
                .recipientName("Owner Customer")
                .phone("+919876543101")
                .addressLine1("Penthouse 101, Marine Drive")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400020")
                .country("India")
                .latitude(new BigDecimal("18.9438"))
                .longitude(new BigDecimal("72.8232"))
                .build());

        AddressEntity address2 = addressRepository.save(AddressEntity.builder()
                .user(attackerCustomer)
                .recipientName("Attacker Customer")
                .phone("+919876543102")
                .addressLine1("Flat 4B, Bandra West")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400050")
                .country("India")
                .latitude(new BigDecimal("19.0596"))
                .longitude(new BigDecimal("72.8295"))
                .build());

        // 7. Create Orders & Shipments
        OrderEntity order1 = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-OWNER-001")
                .user(ownerCustomer)
                .shippingAddress(address1)
                .recipientName("Owner Customer")
                .recipientPhone("+919876543101")
                .addressLine1("Penthouse 101, Marine Drive")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400020")
                .country("India")
                .status(OrderStatus.PROCESSING)
                .subtotal(new BigDecimal("1500.00"))
                .grandTotal(new BigDecimal("1500.00"))
                .build());

        SellerOrderEntity sellerOrder1 = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order1)
                .seller(seller)
                .subOrderNumber("SO-OWNER-001-A")
                .subtotal(new BigDecimal("1500.00"))
                .totalAmount(new BigDecimal("1500.00"))
                .status(SellerOrderStatus.PROCESSING)
                .build());

        shipment1 = shipmentRepository.save(ShipmentEntity.builder()
                .sellerOrder(sellerOrder1)
                .shipmentNumber("SHP-SEC-001")
                .carrierName("BazaarHub Fleet")
                .trackingNumber("TRK-SEC-001")
                .status(ShipmentStatus.IN_TRANSIT)
                .build());

        // Assign shipment1 to assignedDeliveryBoy
        deliveryAssignmentRepository.save(DeliveryAssignmentEntity.builder()
                .shipment(shipment1)
                .deliveryBoy(assignedDeliveryBoy)
                .assignmentStatus(DeliveryAssignmentStatus.ACCEPTED)
                .assignedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build());

        // Seed initial GPS coordinate for shipment1
        deliveryTrackingRepository.save(DeliveryTrackingEntity.builder()
                .shipment(shipment1)
                .deliveryBoy(assignedDeliveryBoy)
                .latitude(new BigDecimal("18.9440"))
                .longitude(new BigDecimal("72.8235"))
                .recordedAt(Instant.now())
                .build());

        // Create Order 2 and Shipment 2 for attacker customer (assigned to unassignedDeliveryBoy)
        OrderEntity order2 = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-ATTACKER-002")
                .user(attackerCustomer)
                .shippingAddress(address2)
                .recipientName("Attacker Customer")
                .recipientPhone("+919876543102")
                .addressLine1("Flat 4B, Bandra West")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400050")
                .country("India")
                .status(OrderStatus.PROCESSING)
                .subtotal(new BigDecimal("2200.00"))
                .grandTotal(new BigDecimal("2200.00"))
                .build());

        SellerOrderEntity sellerOrder2 = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order2)
                .seller(seller)
                .subOrderNumber("SO-ATTACKER-002-A")
                .subtotal(new BigDecimal("2200.00"))
                .totalAmount(new BigDecimal("2200.00"))
                .status(SellerOrderStatus.PROCESSING)
                .build());

        shipment2 = shipmentRepository.save(ShipmentEntity.builder()
                .sellerOrder(sellerOrder2)
                .shipmentNumber("SHP-SEC-002")
                .carrierName("BazaarHub Fleet")
                .trackingNumber("TRK-SEC-002")
                .status(ShipmentStatus.IN_TRANSIT)
                .build());

        deliveryAssignmentRepository.save(DeliveryAssignmentEntity.builder()
                .shipment(shipment2)
                .deliveryBoy(unassignedDeliveryBoy)
                .assignmentStatus(DeliveryAssignmentStatus.ACCEPTED)
                .assignedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @DisplayName("Case 1: Shipment owner customer can access tracking")
    void testShipmentOwnerCanAccessTracking_Success() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + ownerCustomerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shipmentId").value(shipment1.getId()))
                .andExpect(jsonPath("$.data.latitude").value(18.9440))
                .andExpect(jsonPath("$.data.longitude").value(72.8235));
    }

    @Test
    @DisplayName("Case 2: Different customer cannot access tracking (BOLA blocked)")
    void testDifferentCustomerCannotAccessTracking_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + attackerCustomerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to access tracking for shipment ID: " + shipment1.getId()));
    }

    @Test
    @DisplayName("Case 3: Assigned delivery agent can access tracking")
    void testAssignedDeliveryAgentCanAccessTracking_Success() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + assignedRiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shipmentId").value(shipment1.getId()))
                .andExpect(jsonPath("$.data.latitude").value(18.9440));
    }

    @Test
    @DisplayName("Case 4: Unassigned delivery agent cannot access tracking (BOLA blocked)")
    void testUnassignedDeliveryAgentCannotAccessTracking_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + unassignedRiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to access tracking for shipment ID: " + shipment1.getId()));
    }

    @Test
    @DisplayName("Case 5: Authorized admin access follows existing policy")
    void testAdminCanAccessTracking_Success() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shipmentId").value(shipment1.getId()));
    }

    @Test
    @DisplayName("Case 6: Unauthorized access returns HTTP 403 with standardized error payload")
    void testUnauthorizedAccess_ReturnsUniform403ErrorPayload() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId())
                        .header("Authorization", "Bearer " + attackerCustomerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("You are not authorized to access tracking for shipment ID: " + shipment1.getId()));
    }

    @Test
    @DisplayName("Case 7: Unauthorized user cannot access estimated arrival; owner customer succeeds")
    void testEstimatedArrival_SecurityEnforcement() throws Exception {
        // Owner customer succeeds (200 OK)
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId() + "/estimate")
                        .header("Authorization", "Bearer " + ownerCustomerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Different customer receives 403 Forbidden
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId() + "/estimate")
                        .header("Authorization", "Bearer " + attackerCustomerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to access tracking for shipment ID: " + shipment1.getId()));

        // Unassigned delivery agent receives 403 Forbidden
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId() + "/estimate")
                        .header("Authorization", "Bearer " + unassignedRiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Assigned delivery agent succeeds (200 OK)
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId() + "/estimate")
                        .header("Authorization", "Bearer " + assignedRiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Admin succeeds (200 OK)
        mockMvc.perform(get("/api/v1/delivery/tracking/" + shipment1.getId() + "/estimate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Case 8: Delivery boy route estimate assignment enforcement (Assigned succeeds, different agent 403)")
    void testDeliveryBoyRouteEstimate_AssignmentEnforcement() throws Exception {
        // 1. Assigned agent succeeds (200 OK)
        mockMvc.perform(get("/api/v1/delivery/route-estimate/" + shipment1.getId())
                        .header("Authorization", "Bearer " + assignedRiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Different agent receives HTTP 403 Forbidden
        mockMvc.perform(get("/api/v1/delivery/route-estimate/" + shipment1.getId())
                        .header("Authorization", "Bearer " + unassignedRiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You are not authorized to access tracking for shipment ID: " + shipment1.getId()));

        // 3. Admin succeeds (200 OK)
        mockMvc.perform(get("/api/v1/delivery/route-estimate/" + shipment1.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // --- Helpers ---

    private long onboardAndVerifyRider(String riderAuthToken, String adminAuthToken, String vehicleNum) throws Exception {
        DeliveryBoyOnboardRequest onboardReq = DeliveryBoyOnboardRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber(vehicleNum)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/delivery/onboard")
                        .header("Authorization", "Bearer " + riderAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onboardReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        long riderDbId = node.path("data").path("id").asLong();

        AdminVerifyDeliveryBoyRequest verifyReq = AdminVerifyDeliveryBoyRequest.builder()
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .remarks("Verified credentials")
                .build();

        mockMvc.perform(patch("/api/v1/admin/delivery/boys/" + riderDbId + "/verify")
                        .header("Authorization", "Bearer " + adminAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk());

        return riderDbId;
    }

    private String registerAndLogin(String email, String password, String fullName, String phone) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .fullName(fullName)
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

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
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
