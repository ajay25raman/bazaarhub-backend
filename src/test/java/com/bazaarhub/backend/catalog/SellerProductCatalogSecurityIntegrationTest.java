package com.bazaarhub.backend.catalog;

import com.bazaarhub.backend.catalog.application.dto.request.ProductImageOrderRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductImageRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantUpdateRequest;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.*;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Targeted Integration Test suite validating Broken Object Level Authorization (BOLA / IDOR)
 * defense across Seller Product Catalog endpoints (Variants & Images).
 * Stage 5 REM-SEC-04 verification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SellerProductCatalogSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataUserDeviceRepository userDeviceRepository;

    @Autowired
    private SpringDataRoleRepository roleRepository;

    @Autowired
    private SpringDataPermissionRepository permissionRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataBrandRepository brandRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataStoreRepository storeRepository;

    @Autowired
    private SpringDataSellerDocumentRepository documentRepository;

    @Autowired
    private SpringDataSellerBankAccountRepository bankAccountRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductImageRepository productImageRepository;

    @Autowired
    private SpringDataProductVariantRepository productVariantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String seller1Token;
    private String seller2Token;
    private String adminToken;
    private String customerToken;

    private Long seller1ProductId;
    private Long seller1VariantId;
    private Long seller1ImageId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean database respecting foreign key hierarchies
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
        productVariantRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM user_roles");
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Roles & Permissions setup
        PermissionEntity prodCreatePerm = permissionRepository.findByName("PRODUCT_CREATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("PRODUCT_CREATE").description("Create products").build()));
        PermissionEntity prodUpdatePerm = permissionRepository.findByName("PRODUCT_UPDATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("PRODUCT_UPDATE").description("Update products").build()));
        PermissionEntity prodReadPerm = permissionRepository.findByName("PRODUCT_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("PRODUCT_READ").description("Read products").build()));
        PermissionEntity prodModPerm = permissionRepository.findByName("PRODUCT_MODERATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("PRODUCT_MODERATE").description("Moderate products").build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));

        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_SELLER")
                        .description("Seller")
                        .permissions(Set.of(prodCreatePerm, prodUpdatePerm, prodReadPerm))
                        .build()));

        RoleEntity adminRole = roleRepository.findByNameWithPermissions("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator")
                        .permissions(new java.util.HashSet<>(Set.of(prodCreatePerm, prodUpdatePerm, prodReadPerm, prodModPerm)))
                        .build()));
        if (!adminRole.getPermissions().contains(prodUpdatePerm)) {
            adminRole.getPermissions().addAll(Set.of(prodCreatePerm, prodUpdatePerm, prodReadPerm, prodModPerm));
            adminRole = roleRepository.save(adminRole);
        }

        // 2. Customer
        customerToken = registerAndLogin("Charlie Customer", "customer@bazaarhub.com", "+919100000010", "Password123!");

        // 3. Seller 1
        registerAndLogin("Seller One", "seller1@bazaarhub.com", "+919100000011", "Password123!");
        UserEntity seller1User = userRepository.findByIdentifierWithRolesAndPermissions("seller1@bazaarhub.com").orElseThrow();
        seller1User.getRoles().add(sellerRole);
        userRepository.save(seller1User);
        SellerEntity seller1Entity = sellerRepository.save(SellerEntity.builder()
                .user(seller1User)
                .businessName("Seller 1 Enterprises")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build());
        seller1Token = login("seller1@bazaarhub.com", "Password123!");

        // 4. Seller 2
        registerAndLogin("Seller Two", "seller2@bazaarhub.com", "+919100000012", "Password123!");
        UserEntity seller2User = userRepository.findByIdentifierWithRolesAndPermissions("seller2@bazaarhub.com").orElseThrow();
        seller2User.getRoles().add(sellerRole);
        userRepository.save(seller2User);
        sellerRepository.save(SellerEntity.builder()
                .user(seller2User)
                .businessName("Seller 2 Mart")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build());
        seller2Token = login("seller2@bazaarhub.com", "Password123!");

        // 5. Admin
        registerAndLogin("Admin User", "admin@bazaarhub.com", "+919100000013", "Password123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin@bazaarhub.com", "Password123!");

        // 6. Category & Brand
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Electronics")
                .slug("electronics")
                .status(CatalogStatus.ACTIVE)
                .displayOrder(1)
                .build());

        BrandEntity brand = brandRepository.save(BrandEntity.builder()
                .name("BrandCorp")
                .slug("brandcorp")
                .status(CatalogStatus.ACTIVE)
                .build());

        // 7. Seed Seller 1 Product, Variant & Image
        ProductEntity product1 = productRepository.save(ProductEntity.builder()
                .seller(seller1Entity)
                .category(category)
                .brand(brand)
                .name("Seller 1 Laptop")
                .slug("seller-1-laptop")
                .description("High end laptop")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());
        seller1ProductId = product1.getId();

        ProductVariantEntity variant1 = productVariantRepository.save(ProductVariantEntity.builder()
                .product(product1)
                .sku("SKU-S1-LAPTOP-16GB")
                .barcode("1234567890123")
                .variantName("16GB RAM / 512GB SSD")
                .mrp(new BigDecimal("99999.00"))
                .sellingPrice(new BigDecimal("89999.00"))
                .weight(new BigDecimal("2.000"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());
        seller1VariantId = variant1.getId();

        ProductImageEntity image1 = productImageRepository.save(ProductImageEntity.builder()
                .product(product1)
                .imageUrl("https://example.com/laptop-main.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build());
        seller1ImageId = image1.getId();
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

    // =========================================================================
    // POSITIVE ACCESS (Owner Seller 1)
    // =========================================================================

    @Test
    @DisplayName("Owner Seller 1 can list their own product variants")
    void ownerCanListVariants() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", seller1ProductId)
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-S1-LAPTOP-16GB"));
    }

    @Test
    @DisplayName("Owner Seller 1 can get a specific variant of their product")
    void ownerCanGetSpecificVariant() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants/{variantId}", seller1ProductId, seller1VariantId)
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(seller1VariantId))
                .andExpect(jsonPath("$.data.sku").value("SKU-S1-LAPTOP-16GB"));
    }

    @Test
    @DisplayName("Owner Seller 1 can list their own product images")
    void ownerCanListImages() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", seller1ProductId)
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].imageUrl").value("https://example.com/laptop-main.jpg"));
    }

    // =========================================================================
    // BOLA / IDOR ISOLATION (Seller 2 vs Seller 1)
    // =========================================================================

    @Test
    @DisplayName("Seller 2 CANNOT list Seller 1's product variants -> 403 Forbidden")
    void otherSellerCannotListVariants() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", seller1ProductId)
                        .header("Authorization", "Bearer " + seller2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT get a specific variant of Seller 1's product -> 403 Forbidden")
    void otherSellerCannotGetSpecificVariant() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants/{variantId}", seller1ProductId, seller1VariantId)
                        .header("Authorization", "Bearer " + seller2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT list Seller 1's product images -> 403 Forbidden")
    void otherSellerCannotListImages() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", seller1ProductId)
                        .header("Authorization", "Bearer " + seller2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT create a variant on Seller 1's product -> 403 Forbidden")
    void otherSellerCannotCreateVariant() throws Exception {
        ProductVariantCreateRequest request = ProductVariantCreateRequest.builder()
                .sku("SKU-ATTACK-01")
                .variantName("Attack Variant")
                .mrp(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("90.00"))
                .weight(new BigDecimal("0.500"))
                .build();

        mockMvc.perform(post("/api/v1/seller/products/{productId}/variants", seller1ProductId)
                        .header("Authorization", "Bearer " + seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT update a variant on Seller 1's product -> 403 Forbidden")
    void otherSellerCannotUpdateVariant() throws Exception {
        ProductVariantUpdateRequest request = ProductVariantUpdateRequest.builder()
                .sku("SKU-S1-LAPTOP-16GB-MOD")
                .variantName("Updated Variant")
                .mrp(new BigDecimal("99999.00"))
                .sellingPrice(new BigDecimal("85000.00"))
                .weight(new BigDecimal("2.000"))
                .taxRate(new BigDecimal("18.00"))
                .build();

        mockMvc.perform(put("/api/v1/seller/products/{productId}/variants/{variantId}", seller1ProductId, seller1VariantId)
                        .header("Authorization", "Bearer " + seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT add an image to Seller 1's product -> 403 Forbidden")
    void otherSellerCannotAddImage() throws Exception {
        ProductImageRequest request = ProductImageRequest.builder()
                .imageUrl("https://example.com/hacked.jpg")
                .isPrimary(false)
                .displayOrder(1)
                .build();

        mockMvc.perform(post("/api/v1/seller/products/{productId}/images", seller1ProductId)
                        .header("Authorization", "Bearer " + seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT set primary image on Seller 1's product -> 403 Forbidden")
    void otherSellerCannotSetPrimaryImage() throws Exception {
        mockMvc.perform(patch("/api/v1/seller/products/{productId}/images/{imageId}/primary", seller1ProductId, seller1ImageId)
                        .header("Authorization", "Bearer " + seller2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Seller 2 CANNOT update image display order on Seller 1's product -> 403 Forbidden")
    void otherSellerCannotUpdateImageOrder() throws Exception {
        ProductImageOrderRequest request = ProductImageOrderRequest.builder()
                .displayOrder(5)
                .build();

        mockMvc.perform(patch("/api/v1/seller/products/{productId}/images/{imageId}/order", seller1ProductId, seller1ImageId)
                        .header("Authorization", "Bearer " + seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // ADMIN ACCESS PRESERVATION
    // =========================================================================

    @Test
    @DisplayName("Admin CAN inspect any seller's product variants and images -> 200 OK")
    void adminCanInspectAnySellerCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", seller1ProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants/{variantId}", seller1ProductId, seller1VariantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(seller1VariantId));

        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", seller1ProductId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // =========================================================================
    // EDGE CASES: 404 & 401 & 403 (Customer)
    // =========================================================================

    @Test
    @DisplayName("Non-existent product ID returns 404 Not Found")
    void nonExistentProductReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", 999999L)
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", 999999L)
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 Unauthorized")
    void unauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", seller1ProductId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", seller1ProductId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Customer role without seller privileges returns 403 Forbidden")
    void customerCannotAccessSellerEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/seller/products/{productId}/variants", seller1ProductId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/seller/products/{productId}/images", seller1ProductId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
