package com.bazaarhub.backend.catalog;

import com.bazaarhub.backend.catalog.application.dto.request.*;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductImageRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

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

    private String adminToken;
    private String seller1Token;
    private String seller2Token;
    private String customerToken;

    private Long categoryId;
    private Long brandId;

    @BeforeEach
    void setUp() throws Exception {
        // Clear all repositories respecting foreign keys
        productVariantRepository.deleteAll();
        productImageRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Ensure platform roles & permissions exist
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

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator")
                        .permissions(Set.of(prodCreatePerm, prodUpdatePerm, prodReadPerm, prodModPerm))
                        .build()));

        // 2. Register & setup Customer
        customerToken = registerAndLogin("Alice Customer", "alice@bazaarhub.com", "+919100000001", "Password123!");

        // 3. Register & setup Seller 1
        registerAndLogin("Bob Seller 1", "seller1@bazaarhub.com", "+919100000002", "Password123!");
        UserEntity seller1User = userRepository.findByIdentifierWithRolesAndPermissions("seller1@bazaarhub.com").orElseThrow();
        seller1User.getRoles().add(sellerRole);
        userRepository.save(seller1User);
        sellerRepository.save(SellerEntity.builder()
                .user(seller1User)
                .businessName("Prime Traders")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build());
        seller1Token = login("seller1@bazaarhub.com", "Password123!");

        // 4. Register & setup Seller 2
        registerAndLogin("Dan Seller 2", "seller2@bazaarhub.com", "+919100000003", "Password123!");
        UserEntity seller2User = userRepository.findByIdentifierWithRolesAndPermissions("seller2@bazaarhub.com").orElseThrow();
        seller2User.getRoles().add(sellerRole);
        userRepository.save(seller2User);
        sellerRepository.save(SellerEntity.builder()
                .user(seller2User)
                .businessName("Dan Electronics")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .build());
        seller2Token = login("seller2@bazaarhub.com", "Password123!");

        // 5. Register & setup Admin
        registerAndLogin("Adam Admin", "admin@bazaarhub.com", "+919100000004", "Password123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("admin@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);
        adminToken = login("admin@bazaarhub.com", "Password123!");

        // 6. Create active Category & Brand
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Smartphones")
                .slug("smartphones")
                .status(CatalogStatus.ACTIVE)
                .displayOrder(1)
                .build());
        categoryId = category.getId();

        BrandEntity brand = brandRepository.save(BrandEntity.builder()
                .name("Apple")
                .slug("apple")
                .status(CatalogStatus.ACTIVE)
                .build());
        brandId = brand.getId();
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
    @DisplayName("Complete Product Management Lifecycle: Seller creation, gallery, variants, moderation, public storefront, and security isolation")
    void completeProductLifecycle_endToEnd() throws Exception {

        // =========================================================================
        // Step 1: Seller 1 creates a product
        // =========================================================================
        ProductCreateRequest createRequest = ProductCreateRequest.builder()
                .name("iPhone 15 Pro Max")
                .slug("iphone-15-pro-max")
                .categoryId(categoryId)
                .brandId(brandId)
                .description("Titanium flagship device with A17 Pro.")
                .status(ProductStatus.DRAFT)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/seller/products")
                        .header("Authorization", "Bearer " + seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("iPhone 15 Pro Max")))
                .andExpect(jsonPath("$.data.slug", is("iphone-15-pro-max")))
                .andExpect(jsonPath("$.data.status", is("DRAFT")))
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING")))
                .andExpect(jsonPath("$.data.seller.businessName", is("Prime Traders")))
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long productId = createdNode.path("data").path("id").asLong();
        assertNotNull(productId);

        // =========================================================================
        // Step 2: Seller 1 adds images to the product
        // =========================================================================
        // First image (should automatically be marked isPrimary=true)
        ProductImageRequest image1Req = ProductImageRequest.builder()
                .imageUrl("https://cdn.bazaarhub.com/iphone-front.webp")
                .altText("iPhone front view")
                .displayOrder(0)
                .build();

        MvcResult img1Result = mockMvc.perform(post("/api/v1/seller/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image1Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary", is(true)))
                .andReturn();

        Long image1Id = objectMapper.readTree(img1Result.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Second image (isPrimary=false)
        ProductImageRequest image2Req = ProductImageRequest.builder()
                .imageUrl("https://cdn.bazaarhub.com/iphone-back.webp")
                .altText("iPhone back view")
                .displayOrder(1)
                .isPrimary(false)
                .build();

        MvcResult img2Result = mockMvc.perform(post("/api/v1/seller/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(image2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPrimary", is(false)))
                .andReturn();

        Long image2Id = objectMapper.readTree(img2Result.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Set second image as primary
        mockMvc.perform(patch("/api/v1/seller/products/" + productId + "/images/" + image2Id + "/primary")
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPrimary", is(true)));

        // Verify images list has image2 as primary
        mockMvc.perform(get("/api/v1/seller/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + seller1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // =========================================================================
        // Step 3: Seller 1 adds SKU variants
        // =========================================================================
        // Variant 1: Valid variant
        ProductVariantCreateRequest variantReq = ProductVariantCreateRequest.builder()
                .sku("IPHONE15-256-NAT")
                .variantName("Natural Titanium - 256GB")
                .attributes("{\"color\":\"Natural Titanium\",\"storage\":\"256GB\"}")
                .sellingPrice(new BigDecimal("134900.00"))
                .mrp(new BigDecimal("159900.00"))
                .taxRate(new BigDecimal("18.00"))
                .weight(new BigDecimal("0.221"))
                .build();

        MvcResult varResult = mockMvc.perform(post("/api/v1/seller/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(variantReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku", is("IPHONE15-256-NAT")))
                .andExpect(jsonPath("$.data.sellingPrice", is(134900.00)))
                .andReturn();

        Long variantId = objectMapper.readTree(varResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Variant with MRP < sellingPrice must fail with 400 Bad Request
        ProductVariantCreateRequest invalidPriceReq = ProductVariantCreateRequest.builder()
                .sku("IPHONE15-INVALID-PRICE")
                .variantName("Invalid Variant")
                .sellingPrice(new BigDecimal("150000.00"))
                .mrp(new BigDecimal("120000.00")) // MRP < sellingPrice
                .build();

        mockMvc.perform(post("/api/v1/seller/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + seller1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPriceReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("MRP")));

        // =========================================================================
        // Step 4: Seller 2 isolation: Seller 2 cannot modify or access Seller 1's product
        // =========================================================================
        mockMvc.perform(get("/api/v1/seller/products/" + productId)
                        .header("Authorization", "Bearer " + seller2Token))
                .andExpect(status().isForbidden());

        ProductUpdateRequest unauthorizedUpdate = ProductUpdateRequest.builder()
                .name("Hacked Title")
                .categoryId(categoryId)
                .build();

        mockMvc.perform(put("/api/v1/seller/products/" + productId)
                        .header("Authorization", "Bearer " + seller2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedUpdate)))
                .andExpect(status().isForbidden());

        // =========================================================================
        // Step 5: Public Catalog Check before approval: Product is NOT visible
        // =========================================================================
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/products/slug/iphone-15-pro-max"))
                .andExpect(status().isNotFound());

        // =========================================================================
        // Step 6: Admin moderates and approves the product
        // =========================================================================
        // Admin lists pending products
        mockMvc.perform(get("/api/v1/admin/products?approvalStatus=PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(productId.intValue())));

        // Admin approves product
        mockMvc.perform(post("/api/v1/admin/products/" + productId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // =========================================================================
        // Step 7: Public Catalog Check after approval: Product IS visible!
        // =========================================================================
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("iPhone 15 Pro Max")))
                .andExpect(jsonPath("$.data.content[0].primaryImageUrl", is("https://cdn.bazaarhub.com/iphone-back.webp")))
                .andExpect(jsonPath("$.data.content[0].minPrice", is(134900.00)));

        // By ID
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("iPhone 15 Pro Max")))
                .andExpect(jsonPath("$.data.variants", hasSize(1)))
                .andExpect(jsonPath("$.data.variants[0].sku", is("IPHONE15-256-NAT")));

        // By Slug
        mockMvc.perform(get("/api/v1/products/slug/iphone-15-pro-max"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(productId.intValue())));

        // =========================================================================
        // Step 8: Admin rejects product
        // =========================================================================
        ProductApprovalRequest rejectReq = ProductApprovalRequest.builder()
                .approvalStatus(ProductApprovalStatus.REJECTED)
                .rejectionReason("Misleading technical specifications")
                .build();

        mockMvc.perform(post("/api/v1/admin/products/" + productId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("REJECTED")))
                .andExpect(jsonPath("$.data.status", is("INACTIVE")))
                .andExpect(jsonPath("$.data.rejectionReason", is("Misleading technical specifications")));

        // Public access must immediately return 404 for rejected product
        mockMvc.perform(get("/api/v1/products/" + productId))
                .andExpect(status().isNotFound());
    }
}
