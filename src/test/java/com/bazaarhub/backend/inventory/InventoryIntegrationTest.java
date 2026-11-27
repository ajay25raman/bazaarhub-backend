package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.*;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.domain.model.DeviceType;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.*;
import com.bazaarhub.backend.inventory.application.dto.request.SafetyStockUpdateRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockAdjustmentRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockInRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockOutRequest;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryMovementRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataStockReservationRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {

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
    private SpringDataStoreRepository storeRepository;

    @Autowired
    private SpringDataSellerDocumentRepository documentRepository;

    @Autowired
    private SpringDataSellerBankAccountRepository bankAccountRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataBrandRepository brandRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductVariantRepository productVariantRepository;

    @Autowired
    private SpringDataInventoryRepository inventoryRepository;

    @Autowired
    private SpringDataInventoryMovementRepository movementRepository;

    @Autowired
    private SpringDataStockReservationRepository reservationRepository;

    private String sellerToken;
    private String otherSellerToken;
    private String adminToken;
    private String customerToken;

    private Long productId;
    private Long variantId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Clean DB respecting FK constraints
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
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM customer_profiles");
        jdbcTemplate.execute("DELETE FROM inventory_movements");
        jdbcTemplate.execute("DELETE FROM stock_reservations");
        jdbcTemplate.execute("DELETE FROM inventories");
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 2. Ensure Permissions exist
        PermissionEntity invRead = permissionRepository.findByName("INVENTORY_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("INVENTORY_READ").description("Read inventory").build()));
        PermissionEntity invUpdate = permissionRepository.findByName("INVENTORY_UPDATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("INVENTORY_UPDATE").description("Update inventory").build()));
        PermissionEntity invAudit = permissionRepository.findByName("INVENTORY_AUDIT")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("INVENTORY_AUDIT").description("Audit inventory").build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").permissions(Set.of(invRead, invUpdate)).build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").permissions(Set.of(invRead, invAudit)).build()));

        // 3. Setup Users & Logins
        customerToken = registerAndLogin("Alice Customer", "alice@buyer.com", "+919876543201", "Password123!");

        // Seller 1
        registerAndLogin("Bob Seller", "bob@seller.com", "+919876543202", "Password123!");
        UserEntity sellerUser = userRepository.findByIdentifierWithRolesAndPermissions("bob@seller.com").orElseThrow();
        sellerUser.getRoles().add(sellerRole);
        userRepository.save(sellerUser);
        SellerEntity seller1 = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Bob Tech Supplies")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());
        sellerToken = login("bob@seller.com", "Password123!");

        // Seller 2 (other seller)
        registerAndLogin("Charlie Seller", "charlie@seller.com", "+919876543203", "Password123!");
        UserEntity seller2User = userRepository.findByIdentifierWithRolesAndPermissions("charlie@seller.com").orElseThrow();
        seller2User.getRoles().add(sellerRole);
        userRepository.save(seller2User);
        sellerRepository.save(SellerEntity.builder()
                .user(seller2User)
                .businessName("Charlie Goods")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());
        otherSellerToken = login("charlie@seller.com", "Password123!");

        // Admin
        registerAndLogin("Dave Admin", "dave@admin.com", "+919876543204", "Password123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("dave@admin.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("dave@admin.com", "Password123!");

        // 4. Setup Category, Product & Variant for Seller 1
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Laptops")
                .slug("laptops")
                .status(CatalogStatus.ACTIVE)
                .build());

        ProductEntity product = productRepository.save(ProductEntity.builder()
                .seller(seller1)
                .category(category)
                .name("ThinkPad X1 Carbon")
                .slug("thinkpad-x1-carbon")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .createdBy(sellerUser)
                .updatedBy(sellerUser)
                .build());
        productId = product.getId();

        ProductVariantEntity variant = productVariantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .sku("THINKPAD-X1-GEN11")
                .variantName("16GB Core i7 512GB")
                .sellingPrice(new BigDecimal("1499.00"))
                .mrp(new BigDecimal("1799.00"))
                .status(CatalogStatus.ACTIVE)
                .createdBy(sellerUser)
                .updatedBy(sellerUser)
                .build());
        variantId = variant.getId();

        // 5. Initialize Inventory for Variant
        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant)
                .totalQuantity(100)
                .reservedQuantity(10)
                .safetyStock(15)
                .version(0L)
                .createdBy(sellerUser)
                .updatedBy(sellerUser)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    // =========================================================================
    // Seller Inventory Endpoints
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/seller/inventory - List inventory for authenticated seller")
    void getSellerInventory_success() throws Exception {
        mockMvc.perform(get("/api/v1/seller/inventory")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sku", is("THINKPAD-X1-GEN11")))
                .andExpect(jsonPath("$.data.content[0].totalQuantity", is(100)))
                .andExpect(jsonPath("$.data.content[0].reservedQuantity", is(10)))
                .andExpect(jsonPath("$.data.content[0].availableQuantity", is(90)))
                .andExpect(jsonPath("$.data.content[0].status", is("IN_STOCK")));
    }

    @Test
    @DisplayName("GET /api/v1/seller/inventory/{variantId} - Get variant inventory details")
    void getVariantInventory_success() throws Exception {
        mockMvc.perform(get("/api/v1/seller/inventory/" + variantId)
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.variantId", is(variantId.intValue())))
                .andExpect(jsonPath("$.data.totalQuantity", is(100)))
                .andExpect(jsonPath("$.data.availableQuantity", is(90)));
    }

    @Test
    @DisplayName("GET /api/v1/seller/inventory/{variantId} - Fails 403 when accessed by another seller")
    void getVariantInventory_failsForOtherSeller() throws Exception {
        mockMvc.perform(get("/api/v1/seller/inventory/" + variantId)
                        .header("Authorization", "Bearer " + otherSellerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/seller/inventory/{variantId}/stock-in - Stock replenishment")
    void stockIn_success() throws Exception {
        StockInRequest request = StockInRequest.builder()
                .quantity(50)
                .referenceType("RESTOCK_BATCH")
                .notes("New factory arrival")
                .build();

        mockMvc.perform(post("/api/v1/seller/inventory/" + variantId + "/stock-in")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalQuantity", is(150))) // 100 + 50
                .andExpect(jsonPath("$.data.availableQuantity", is(140))); // 150 - 10

        // Verify movement audit record created
        assertEquals(1, movementRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/seller/inventory/{variantId}/stock-out - Stock reduction")
    void stockOut_success() throws Exception {
        StockOutRequest request = StockOutRequest.builder()
                .quantity(20)
                .referenceType("DAMAGED")
                .notes("Dropped during forklift transit")
                .build();

        mockMvc.perform(post("/api/v1/seller/inventory/" + variantId + "/stock-out")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalQuantity", is(80))) // 100 - 20
                .andExpect(jsonPath("$.data.availableQuantity", is(70)));
    }

    @Test
    @DisplayName("POST /api/v1/seller/inventory/{variantId}/adjustment - Reconcile stock count")
    void adjustStock_success() throws Exception {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .newTotalQuantity(95)
                .reason("End of month audit reconciliation")
                .build();

        mockMvc.perform(post("/api/v1/seller/inventory/" + variantId + "/adjustment")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalQuantity", is(95)))
                .andExpect(jsonPath("$.data.availableQuantity", is(85)));
    }

    @Test
    @DisplayName("PATCH /api/v1/seller/inventory/{variantId}/safety-stock - Update safety stock threshold")
    void updateSafetyStock_success() throws Exception {
        SafetyStockUpdateRequest request = SafetyStockUpdateRequest.builder().safetyStock(20).build();

        mockMvc.perform(patch("/api/v1/seller/inventory/" + variantId + "/safety-stock")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.safetyStock", is(20)));
    }

    @Test
    @DisplayName("GET /api/v1/seller/inventory/{variantId}/movements - View movement audit history")
    void getMovements_success() throws Exception {
        // Perform a stock-in to generate movement
        StockInRequest request = StockInRequest.builder().quantity(10).notes("Initial delivery").build();
        mockMvc.perform(post("/api/v1/seller/inventory/" + variantId + "/stock-in")
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/seller/inventory/" + variantId + "/movements")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].movementType", is("STOCK_IN")))
                .andExpect(jsonPath("$.data.content[0].quantity", is(10)));
    }

    // =========================================================================
    // Admin Inventory Endpoints
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/admin/inventory - Admin lists all inventory across vendors")
    void adminListInventory_success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].sellerBusinessName", is("Bob Tech Supplies")));
    }

    @Test
    @DisplayName("POST /api/v1/admin/inventory/{variantId}/adjustment - Admin authoritative adjustment")
    void adminAdjustStock_success() throws Exception {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .newTotalQuantity(115)
                .reason("Platform audit adjustment")
                .build();

        mockMvc.perform(post("/api/v1/admin/inventory/" + variantId + "/adjustment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalQuantity", is(115)));
    }

    // =========================================================================
    // Public Stock Availability Endpoint
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/products/{productId}/variants/{variantId}/availability - Public check")
    void publicStockAvailability_success() throws Exception {
        mockMvc.perform(get("/api/v1/products/" + productId + "/variants/" + variantId + "/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.variantId", is(variantId.intValue())))
                .andExpect(jsonPath("$.data.sku", is("THINKPAD-X1-GEN11")))
                .andExpect(jsonPath("$.data.status", is("IN_STOCK")))
                .andExpect(jsonPath("$.data.purchasable", is(true)))
                // Verify zero leakage of exact counts
                .andExpect(jsonPath("$.data.totalQuantity").doesNotExist())
                .andExpect(jsonPath("$.data.reservedQuantity").doesNotExist())
                .andExpect(jsonPath("$.data.safetyStock").doesNotExist());
    }

    // =========================================================================
    // Internal Reservation Lifecycle Endpoints
    // =========================================================================

    @Test
    @DisplayName("Internal Reservation Lifecycle: Reserve -> Confirm")
    void reservationLifecycle_reserveAndConfirm() throws Exception {
        StockReserveRequest reserveReq = StockReserveRequest.builder()
                .variantId(variantId)
                .checkoutReference("CHK-LIVE-001")
                .quantity(5)
                .ttlMinutes(15)
                .build();

        // 1. Reserve
        mockMvc.perform(post("/api/v1/internal/inventory/reservations/reserve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.quantity", is(5)));

        // Verify reservedQuantity went from 10 to 15
        InventoryEntity invAfterReserve = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertEquals(15, invAfterReserve.getReservedQuantity());
        assertEquals(85, invAfterReserve.getAvailableQuantity()); // 100 - 15

        // 2. Confirm
        mockMvc.perform(post("/api/v1/internal/inventory/reservations/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("checkoutReference", "CHK-LIVE-001")
                        .param("variantId", variantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));

        // Verify total and reserved quantity both reduced by 5: total=95, reserved=10, available=85
        InventoryEntity invAfterConfirm = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertEquals(95, invAfterConfirm.getTotalQuantity());
        assertEquals(10, invAfterConfirm.getReservedQuantity());
        assertEquals(85, invAfterConfirm.getAvailableQuantity());
    }

    @Test
    @DisplayName("Internal Reservation Lifecycle: Reserve -> Release")
    void reservationLifecycle_reserveAndRelease() throws Exception {
        StockReserveRequest reserveReq = StockReserveRequest.builder()
                .variantId(variantId)
                .checkoutReference("CHK-LIVE-002")
                .quantity(3)
                .ttlMinutes(15)
                .build();

        // 1. Reserve
        mockMvc.perform(post("/api/v1/internal/inventory/reservations/reserve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reserveReq)))
                .andExpect(status().isCreated());

        // 2. Release
        mockMvc.perform(post("/api/v1/internal/inventory/reservations/release")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("checkoutReference", "CHK-LIVE-002")
                        .param("variantId", variantId.toString())
                        .param("reason", "Cart timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RELEASED")));

        // Verify reserved returned back to 10, total still 100
        InventoryEntity invAfterRelease = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertEquals(100, invAfterRelease.getTotalQuantity());
        assertEquals(10, invAfterRelease.getReservedQuantity());
        assertEquals(90, invAfterRelease.getAvailableQuantity());
    }

    // =========================================================================
    // Security & RBAC Rejections
    // =========================================================================

    @Test
    @DisplayName("Customer role cannot perform stock modifications (403 Forbidden)")
    void rbac_customerForbidden() throws Exception {
        StockInRequest request = StockInRequest.builder().quantity(10).build();

        mockMvc.perform(post("/api/v1/seller/inventory/" + variantId + "/stock-in")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to seller inventory is rejected (401 Unauthorized)")
    void rbac_unauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/seller/inventory"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String registerAndLogin(String fullName, String email, String phone, String password) throws Exception {
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

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }
}
