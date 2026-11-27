package com.bazaarhub.backend.order;

import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartItemRepository;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartRepository;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataStockReservationRepository;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.CreateOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

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
    private SpringDataUserDeviceRepository userDeviceRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductVariantRepository variantRepository;

    @Autowired
    private SpringDataInventoryRepository inventoryRepository;

    @Autowired
    private SpringDataStockReservationRepository reservationRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataCartRepository cartRepository;

    @Autowired
    private SpringDataCartItemRepository cartItemRepository;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Autowired
    private SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    private SpringDataOrderStatusHistoryRepository historyRepository;

    private String customer1Token;
    private String customer2Token;
    private String sellerAToken;
    private String sellerBToken;
    private String adminToken;

    private Long customer1Id;
    private Long customer2Id;
    private Long sellerAId;
    private Long sellerBId;

    private AddressEntity customer1Address;
    private AddressEntity customer2Address;

    private ProductVariantEntity variantA1;
    private ProductVariantEntity variantA2;
    private ProductVariantEntity variantB1;

    @BeforeEach
    void setUp() throws Exception {
        // Clean dependent tables
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
        variantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Ensure permissions and roles exist
        PermissionEntity cartRead = ensurePermission("CART_READ", "Read cart");
        PermissionEntity cartUpdate = ensurePermission("CART_UPDATE", "Update cart");
        PermissionEntity orderCreate = ensurePermission("ORDER_CREATE", "Create order");
        PermissionEntity orderRead = ensurePermission("ORDER_READ", "Read order");
        PermissionEntity orderCancel = ensurePermission("ORDER_CANCEL", "Cancel order");
        PermissionEntity sellerOrderManage = ensurePermission("SELLER_ORDER_MANAGE", "Manage seller orders");
        PermissionEntity adminOrderManage = ensurePermission("ADMIN_ORDER_MANAGE", "Manage admin orders");

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .permissions(Set.of(cartRead, cartUpdate, orderCreate, orderRead, orderCancel))
                        .build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_SELLER")
                        .description("Seller")
                        .permissions(Set.of(sellerOrderManage))
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Admin")
                        .permissions(Set.of(orderRead, sellerOrderManage, adminOrderManage))
                        .build()));

        // 2. Register Customers
        customer1Token = registerAndLogin("alice@order.com", "Password@123", "Alice Order", "+919876510001");
        customer1Id = userRepository.findByEmail("alice@order.com").orElseThrow().getId();
        UserEntity customer1User = userRepository.findById(customer1Id).orElseThrow();

        customer2Token = registerAndLogin("bob@order.com", "Password@123", "Bob Order", "+919876510002");
        customer2Id = userRepository.findByEmail("bob@order.com").orElseThrow().getId();
        UserEntity customer2User = userRepository.findById(customer2Id).orElseThrow();

        // Customer addresses
        customer1Address = addressRepository.save(AddressEntity.builder()
                .user(customer1User)
                .addressType("HOME")
                .recipientName("Alice Order")
                .phone("+919876510001")
                .addressLine1("123 Indiranagar 100ft Rd")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560038")
                .country("India")
                .isDefault(true)
                .build());

        customer2Address = addressRepository.save(AddressEntity.builder()
                .user(customer2User)
                .addressType("HOME")
                .recipientName("Bob Order")
                .phone("+919876510002")
                .addressLine1("456 Koramangala 5th Block")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560034")
                .country("India")
                .isDefault(true)
                .build());

        // 3. Register Sellers
        registerAndLogin("sellera@order.com", "Password@123", "Seller A Merchant", "+919876510010");
        UserEntity sellerUserA = userRepository.findByIdentifierWithRolesAndPermissions("sellera@order.com").orElseThrow();
        sellerUserA.getRoles().add(sellerRole);
        userRepository.save(sellerUserA);
        SellerEntity sellerA = sellerRepository.save(SellerEntity.builder()
                .user(sellerUserA)
                .businessName("Seller Alpha Store")
                .businessEmail("sellera@order.com")
                .businessPhone("+919876510010")
                .taxIdentifier("GSTIN11111111")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());
        sellerAId = sellerA.getId();
        sellerAToken = login("sellera@order.com", "Password@123");

        registerAndLogin("sellerb@order.com", "Password@123", "Seller B Merchant", "+919876510020");
        UserEntity sellerUserB = userRepository.findByIdentifierWithRolesAndPermissions("sellerb@order.com").orElseThrow();
        sellerUserB.getRoles().add(sellerRole);
        userRepository.save(sellerUserB);
        SellerEntity sellerB = sellerRepository.save(SellerEntity.builder()
                .user(sellerUserB)
                .businessName("Seller Beta Audio")
                .businessEmail("sellerb@order.com")
                .businessPhone("+919876510020")
                .taxIdentifier("GSTIN22222222")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());
        sellerBId = sellerB.getId();
        sellerBToken = login("sellerb@order.com", "Password@123");

        // 4. Register Admin
        registerAndLogin("admin@order.com", "Password@123", "Dave Admin", "+919876510030");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin@order.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin@order.com", "Password@123");

        // 5. Category & Products
        CategoryEntity electronics = categoryRepository.save(CategoryEntity.builder()
                .name("Consumer Electronics")
                .slug("consumer-electronics")
                .status(CatalogStatus.ACTIVE)
                .build());

        ProductEntity productA = productRepository.save(ProductEntity.builder()
                .name("Alpha Smartphone 15")
                .slug("alpha-smartphone-15")
                .category(electronics)
                .seller(sellerA)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        ProductEntity productB = productRepository.save(ProductEntity.builder()
                .name("Beta Noise Cancelling Headphones")
                .slug("beta-noise-cancelling-headphones")
                .category(electronics)
                .seller(sellerB)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        variantA1 = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-15-128")
                .variantName("128GB Black")
                .sellingPrice(new BigDecimal("50000.00"))
                .mrp(new BigDecimal("55000.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        variantA2 = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-15-256")
                .variantName("256GB Silver")
                .sellingPrice(new BigDecimal("60000.00"))
                .mrp(new BigDecimal("65000.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        variantB1 = variantRepository.save(ProductVariantEntity.builder()
                .product(productB)
                .sku("BETA-NC-BLK")
                .variantName("Matte Black")
                .sellingPrice(new BigDecimal("10000.00"))
                .mrp(new BigDecimal("12000.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        // 6. Inventories
        inventoryRepository.save(InventoryEntity.builder()
                .variant(variantA1)
                .totalQuantity(50)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variantA2)
                .totalQuantity(30)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variantB1)
                .totalQuantity(40)
                .reservedQuantity(0)
                .safetyStock(2)
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
    // 1. ORDER CREATION & MULTI-VENDOR SPLITTING
    // =========================================================================

    @Test
    @DisplayName("Scenario 1, 2, 3, 4, 5: Create multi-vendor order from cart with stock reservation and splitting")
    void testCreateOrder_MultiVendorSplitting_Success() throws Exception {
        // Add item from Seller A
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 2))))
                .andExpect(status().isCreated());

        // Add item from Seller B
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantB1.getId(), 1))))
                .andExpect(status().isCreated());

        // Place order
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(customer1Address.getId())
                .specialInstructions("Handle with care")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value(startsWith("ORD-")))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.subtotal").value(110000.00)) // 2 * 50000 + 1 * 10000
                .andExpect(jsonPath("$.data.taxAmount").value(19800.00)) // 110000 * 18%
                .andExpect(jsonPath("$.data.grandTotal").value(129800.00)) // 110000 + 19800
                .andExpect(jsonPath("$.data.sellerOrders", hasSize(2)))
                .andExpect(jsonPath("$.data.statusHistory", not(empty())))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String orderNumber = root.path("data").path("orderNumber").asText();

        // Verify stock reservation created
        assertTrue(reservationRepository.findByCheckoutReferenceAndVariantId(orderNumber, variantA1.getId()).isPresent());
        assertTrue(reservationRepository.findByCheckoutReferenceAndVariantId(orderNumber, variantB1.getId()).isPresent());

        // Verify cart is converted and items cleared
        CartEntity cart = cartRepository.findByUserId(customer1Id).orElseThrow();
        assertEquals(CartStatus.CONVERTED, cart.getStatus());
        assertEquals(0, cartItemRepository.findByCartId(cart.getId()).size());

        // Verify status history audit records
        assertFalse(historyRepository.findByOrderIdOrderByCreatedAtDesc(root.path("data").path("id").asLong()).isEmpty());
    }

    @Test
    @DisplayName("Scenario 11: Order creation fails if cart is empty")
    void testCreateOrder_FailsWhenCartIsEmpty() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(customer1Address.getId())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("empty")));
    }

    @Test
    @DisplayName("Scenario 12: Order creation fails if delivery address belongs to another customer")
    void testCreateOrder_FailsWhenAddressBelongsToAnotherCustomer() throws Exception {
        // Add item
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        // Use Customer 2's address
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(customer2Address.getId())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 13: Order creation uses default address when addressId is omitted")
    void testCreateOrder_UsesDefaultAddressWhenOmitted() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        // Send request without addressId
        CreateOrderRequest request = CreateOrderRequest.builder().build();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shippingAddress.id").value(customer1Address.getId()));
    }

    @Test
    @DisplayName("Scenario 14: Order creation fails when requested quantity exceeds available stock")
    void testCreateOrder_FailsWhenInsufficientStock() throws Exception {
        // Set stock of variantA1 to 2
        InventoryEntity inv = inventoryRepository.findByVariantId(variantA1.getId()).orElseThrow();
        inv.setTotalQuantity(2);
        inv.setReservedQuantity(0);
        inventoryRepository.save(inv);

        // Add 2 to cart
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 2))))
                .andExpect(status().isCreated());

        // Now someone else reserves 1 unit
        InventoryEntity freshInv = inventoryRepository.findByVariantId(variantA1.getId()).orElseThrow();
        freshInv.setReservedQuantity(1);
        inventoryRepository.save(freshInv);

        // Checkout attempt: requested 2, available 1 -> fails
        CreateOrderRequest request = CreateOrderRequest.builder()
                .addressId(customer1Address.getId())
                .build();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Insufficient stock")));
    }

    // =========================================================================
    // 2. CUSTOMER ORDER QUERIES & DETAIL VIEWS
    // =========================================================================

    @Test
    @DisplayName("Scenario 6, 7, 8, 9: Customer lists orders, filters by status, views details by ID and Number")
    void testCustomerOrderQueries_Success() throws Exception {
        // Place an order first
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = root.path("data").path("id").asLong();
        String orderNumber = root.path("data").path("orderNumber").asText();

        // 1. List customer orders
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].orderNumber").value(orderNumber));

        // 2. Filter by status PENDING
        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // 3. Filter by status DELIVERED (should be empty)
        mockMvc.perform(get("/api/v1/orders")
                        .param("status", "DELIVERED")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", empty()));

        // 4. View details by ID
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.data.sellerOrders", hasSize(1)))
                .andExpect(jsonPath("$.data.sellerOrders[0].items", hasSize(1)));

        // 5. View details by Order Number
        mockMvc.perform(get("/api/v1/orders/by-number/" + orderNumber)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber));
    }

    @Test
    @DisplayName("Scenario 10: Customer cannot view another customer's order (403 Forbidden)")
    void testCustomerCannotViewAnotherCustomerOrder() throws Exception {
        // Customer 1 places order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Customer 2 attempts to view Customer 1's order
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 3. ORDER CANCELLATION & STOCK RESTORATION
    // =========================================================================

    @Test
    @DisplayName("Scenario 15, 18, 19: Customer cancels PENDING order - stock released and cascades to sub-orders")
    void testCancelOrder_Pending_Success() throws Exception {
        // Place order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 3))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = root.path("data").path("id").asLong();
        String orderNumber = root.path("data").path("orderNumber").asText();

        // Verify reserved stock before cancellation
        InventoryEntity invBefore = inventoryRepository.findByVariantId(variantA1.getId()).orElseThrow();
        assertEquals(3, invBefore.getReservedQuantity());

        // Cancel order
        CancelOrderRequest cancelReq = CancelOrderRequest.builder().reason("Changed my mind").build();
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Changed my mind"))
                .andExpect(jsonPath("$.data.sellerOrders[0].status").value("CANCELLED"));

        // Verify reserved stock released
        InventoryEntity invAfter = inventoryRepository.findByVariantId(variantA1.getId()).orElseThrow();
        assertEquals(0, invAfter.getReservedQuantity());
    }

    @Test
    @DisplayName("Scenario 17: Customer cannot cancel order in SHIPPED or DELIVERED status")
    void testCancelOrder_FailsWhenShippedOrDelivered() throws Exception {
        // Place order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Manually update to SHIPPED in DB
        OrderEntity order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        // Attempt cancellation
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("Too late"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be cancelled in status: SHIPPED")));
    }

    // =========================================================================
    // 4. SELLER ORDER MANAGEMENT & FULFILLMENT LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Scenario 20, 21, 22, 23: Seller lists their sub-orders, filters by status, views details")
    void testSellerOrderQueries_Success() throws Exception {
        // Customer places multi-vendor order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantB1.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated());

        // 1. Seller A lists orders -> should see exactly 1 sub-order for Seller Alpha Store
        MvcResult sellerListResult = mockMvc.perform(get("/api/v1/seller/orders")
                        .header("Authorization", "Bearer " + sellerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sellerId").value(sellerAId))
                .andReturn();

        JsonNode root = objectMapper.readTree(sellerListResult.getResponse().getContentAsString());
        Long sellerOrderId = root.path("data").path("content").get(0).path("id").asLong();
        String subOrderNumber = root.path("data").path("content").get(0).path("subOrderNumber").asText();

        // 2. Filter by status
        mockMvc.perform(get("/api/v1/seller/orders")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + sellerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // 3. View details by ID
        mockMvc.perform(get("/api/v1/seller/orders/" + sellerOrderId)
                        .header("Authorization", "Bearer " + sellerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sellerOrderId))
                .andExpect(jsonPath("$.data.items", hasSize(1)));

        // 4. View details by sub-order number
        mockMvc.perform(get("/api/v1/seller/orders/by-number/" + subOrderNumber)
                        .header("Authorization", "Bearer " + sellerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subOrderNumber").value(subOrderNumber));
    }

    @Test
    @DisplayName("Scenario 24: Seller cannot view or manage another seller's sub-order (403 Forbidden)")
    void testSellerCannotViewAnotherSellerOrder() throws Exception {
        // Place order with Seller A item
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated());

        SellerOrderEntity sellerAOrder = sellerOrderRepository.findAll().get(0);

        // Seller B attempts to view Seller A's sub-order
        mockMvc.perform(get("/api/v1/seller/orders/" + sellerAOrder.getId())
                        .header("Authorization", "Bearer " + sellerBToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 25, 26, 29: Seller transitions sub-order lifecycle and parent order automatically synchronizes")
    void testSellerOrderLifecycle_TransitionsAndParentSync() throws Exception {
        // Place order with single vendor Seller A
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        Long parentOrderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();
        SellerOrderEntity sellerOrder = sellerOrderRepository.findAll().get(0);
        Long soId = sellerOrder.getId();

        // 1. PENDING -> CONFIRMED
        mockMvc.perform(patch("/api/v1/seller/orders/" + soId + "/status")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("CONFIRMED", "Confirmed by merchant"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        assertEquals(OrderStatus.CONFIRMED, orderRepository.findById(parentOrderId).orElseThrow().getStatus());

        // 2. CONFIRMED -> PROCESSING
        mockMvc.perform(patch("/api/v1/seller/orders/" + soId + "/status")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("PROCESSING", "Item packing in progress"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        // 3. PROCESSING -> SHIPPED
        mockMvc.perform(patch("/api/v1/seller/orders/" + soId + "/status")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("SHIPPED", "Dispatched with courier"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        assertEquals(OrderStatus.SHIPPED, orderRepository.findById(parentOrderId).orElseThrow().getStatus());

        // 5. SHIPPED -> DELIVERED (Automatic sync parent order to DELIVERED)
        mockMvc.perform(patch("/api/v1/seller/orders/" + soId + "/status")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("DELIVERED", "Package delivered to customer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        assertEquals(OrderStatus.DELIVERED, orderRepository.findById(parentOrderId).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Scenario 27: Seller cannot perform invalid status transition (400 Bad Request)")
    void testSellerOrder_InvalidStatusTransition() throws Exception {
        // Place order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated());

        SellerOrderEntity sellerOrder = sellerOrderRepository.findAll().get(0);

        // Attempt illegal transition: PENDING -> DELIVERED
        mockMvc.perform(patch("/api/v1/seller/orders/" + sellerOrder.getId() + "/status")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("DELIVERED", "Skipping"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid seller sub-order status transition")));
    }

    @Test
    @DisplayName("Scenario 28: Seller cancels their sub-order before shipment - stock restored")
    void testSellerCancelSubOrder_Success() throws Exception {
        // Place order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated());

        SellerOrderEntity sellerOrder = sellerOrderRepository.findAll().get(0);

        // Seller cancels sub-order
        mockMvc.perform(post("/api/v1/seller/orders/" + sellerOrder.getId() + "/cancel")
                        .header("Authorization", "Bearer " + sellerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelOrderRequest("Out of stock at merchant warehouse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Out of stock at merchant warehouse"));

        // Verify stock released
        InventoryEntity inv = inventoryRepository.findByVariantId(variantA1.getId()).orElseThrow();
        assertEquals(0, inv.getReservedQuantity());
    }

    // =========================================================================
    // 5. ADMIN ORDER MANAGEMENT & AUDIT OVERSIGHT
    // =========================================================================

    @Test
    @DisplayName("Scenario 30, 31, 32, 33, 34: Admin lists orders, views details, updates status, and views history")
    void testAdminOrderManagement_Success() throws Exception {
        // Place order
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA1.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult createResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(customer1Address.getId(), null))))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 1. Admin lists all orders
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // 2. Admin views order details
        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.sellerOrders", hasSize(1)));

        // 3. Admin administratively updates status (CONFIRMED)
        mockMvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateOrderStatusRequest("CONFIRMED", "Admin manual confirmation"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // 4. Admin lists all seller sub-orders across vendors
        mockMvc.perform(get("/api/v1/admin/orders/seller-orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // 5. Admin views order status audit history
        mockMvc.perform(get("/api/v1/admin/orders/" + orderId + "/history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    @DisplayName("Scenario 35: Unauthenticated access to order endpoints rejected with 401 Unauthorized")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/seller/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());
    }
}
