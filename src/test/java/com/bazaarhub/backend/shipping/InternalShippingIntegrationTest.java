package com.bazaarhub.backend.shipping;

import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
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
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import com.bazaarhub.backend.shipping.application.dto.request.CancelShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalShippingIntegrationTest {

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
    private SpringDataStoreRepository storeRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Autowired
    private SpringDataShipmentRepository shipmentRepository;

    private String customerToken;
    private String sellerToken;
    private String adminToken;

    private UserEntity customerUser;
    private UserEntity sellerUser;
    private UserEntity adminUser;

    private SellerEntity seller;
    private OrderEntity order;
    private SellerOrderEntity sellerOrder;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        // Ensure roles exist
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // 1. Register Customer
        customerToken = registerAndLogin("shipping.customer@bazaarhub.com", "Password@123", "Customer Ajay", "+919876544001");
        customerUser = userRepository.findByEmail("shipping.customer@bazaarhub.com").orElseThrow();

        // 2. Register Seller
        registerAndLogin("shipping.seller@bazaarhub.com", "Password@123", "Merchant Ramesh", "+919876544002");
        sellerUser = userRepository.findByEmail("shipping.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("shipping.seller@bazaarhub.com", "Password@123");

        seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Bazaar Electronics Store")
                .businessEmail("shipping.seller@bazaarhub.com")
                .businessPhone("+919876544002")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        storeRepository.save(StoreEntity.builder()
                .seller(seller)
                .storeName("Bazaar Electronics Store")
                .slug("bazaar-electronics-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.ACTIVE)
                .build());

        // 3. Register Admin
        registerAndLogin("shipping.admin@bazaarhub.com", "Password@123", "Super Logistics Admin", "+919876544003");
        adminUser = userRepository.findByEmail("shipping.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("shipping.admin@bazaarhub.com", "Password@123");

        // 4. Create Order & Seller Order
        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customerUser)
                .recipientName("Customer Ajay")
                .phone("+919876544001")
                .addressLine1("123 Indiranagar 100ft Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .country("India")
                .latitude(new BigDecimal("12.9784000"))
                .longitude(new BigDecimal("77.6408000"))
                .build());

        order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-SHIP-" + System.currentTimeMillis())
                .user(customerUser)
                .shippingAddress(address)
                .recipientName("Customer Ajay")
                .recipientPhone("+919876544001")
                .addressLine1(address.getAddressLine1())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .subtotal(new BigDecimal("800.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .grandTotal(new BigDecimal("840.00"))
                .paymentStatus(PaymentStatus.PAID)
                .status(OrderStatus.CONFIRMED)
                .placedAt(Instant.now())
                .build());

        sellerOrder = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-SHIP-" + System.currentTimeMillis())
                .subtotal(new BigDecimal("800.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("840.00"))
                .status(SellerOrderStatus.CONFIRMED)
                .build());
    }

    @Test
    @DisplayName("End-to-End Internal Shipping & Shipment Tracking Lifecycle Test")
    void testCompleteShippingLifecycle() throws Exception {

        // Step 1: Calculate Shipping Rate Quote
        ShippingRateQuoteRequest quoteRequest = ShippingRateQuoteRequest.builder()
                .sellerOrderId(sellerOrder.getId())
                .weightKg(new BigDecimal("1.500"))
                .build();

        mockMvc.perform(post("/api/v1/shipments/rate-quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baseCharge").value(40.00))
                .andExpect(jsonPath("$.data.freeShippingApplied").value(true)); // subtotal 800 >= 500

        // Step 2: Seller Creates Internal Shipment for Seller Order
        CreateInternalShipmentRequest createRequest = CreateInternalShipmentRequest.builder()
                .sellerOrderId(sellerOrder.getId())
                .carrierName("BazaarHub Express")
                .packageWeight(new BigDecimal("1.500"))
                .lengthCm(new BigDecimal("25.0"))
                .widthCm(new BigDecimal("18.0"))
                .heightCm(new BigDecimal("12.0"))
                .isFragile(false)
                .deliveryNotes("Fragile handle with care")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.shipmentNumber", startsWith("SHP-")))
                .andExpect(jsonPath("$.data.trackingNumber", startsWith("BZH-")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.carrierName").value("BazaarHub Express"))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        Long shipmentId = createdNode.path("id").asLong();
        String shipmentNumber = createdNode.path("shipmentNumber").asText();
        String trackingNumber = createdNode.path("trackingNumber").asText();

        assertNotNull(shipmentNumber);
        assertNotNull(trackingNumber);

        // Step 3: Duplicate Shipment Prevention (409 Conflict)
        mockMvc.perform(post("/api/v1/shipments")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // Step 4: Seller Retrieves Shipment Details
        mockMvc.perform(get("/api/v1/shipments/seller/" + shipmentId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(shipmentId))
                .andExpect(jsonPath("$.data.sellerId").value(seller.getId()));

        // Step 5: Customer Retrieves All Shipments for Parent Order
        mockMvc.perform(get("/api/v1/shipments/order/" + order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(shipmentId));

        // Step 6: Seller Marks Parcel Ready For Pickup
        ReadyForPickupRequest readyRequest = ReadyForPickupRequest.builder()
                .packageWeight(new BigDecimal("1.550"))
                .notes("Packed in standard box at Dispatch Counter 1")
                .build();

        mockMvc.perform(post("/api/v1/shipments/" + shipmentId + "/ready-for-pickup")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(readyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"));

        // Step 7: Public Tracking Lookup By Tracking Number (No Auth Token)
        mockMvc.perform(get("/api/v1/shipments/track/" + trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingNumber").value(trackingNumber))
                .andExpect(jsonPath("$.data.shipmentNumber").value(shipmentNumber))
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"))
                .andExpect(jsonPath("$.data.destinationCity").value("Bengaluru"))
                .andExpect(jsonPath("$.data.milestones", hasSize(greaterThanOrEqualTo(2))));

        // Step 8: Authenticated Customer Full Tracking History
        mockMvc.perform(get("/api/v1/shipments/" + shipmentId + "/tracking-history")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shipmentId").value(shipmentId))
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PICKUP"))
                .andExpect(jsonPath("$.data.milestones", hasSize(greaterThanOrEqualTo(2))));

        // Step 9: Customer Lists Their Shipments
        mockMvc.perform(get("/api/v1/customer/shipments")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Step 10: Admin Lists All Shipments & Active Shipments
        mockMvc.perform(get("/api/v1/admin/shipments")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", shipmentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/admin/shipments/active")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Step 11: Admin Cancels Shipment
        CancelShipmentRequest cancelRequest = CancelShipmentRequest.builder()
                .reason("Customer request cancellation before pickup")
                .build();

        mockMvc.perform(post("/api/v1/admin/shipments/" + shipmentId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    private String registerAndLogin(String email, String password, String fullName, String phone) throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .email(email)
                .password(password)
                .confirmPassword(password)
                .fullName(fullName)
                .phone(phone)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    private String login(String email, String password) throws Exception {
        LoginRequest login = LoginRequest.builder()
                .identifier(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
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
