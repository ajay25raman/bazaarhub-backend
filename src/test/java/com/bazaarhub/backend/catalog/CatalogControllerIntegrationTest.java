package com.bazaarhub.backend.catalog;

import com.bazaarhub.backend.catalog.application.dto.request.BrandCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.BrandStatusUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.BrandUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.CategoryCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.CategoryStatusUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.CategoryUpdateRequest;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
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
class CatalogControllerIntegrationTest {

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
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure products table stub exists in test H2 for referential check
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS products (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                category_id BIGINT,
                brand_id BIGINT,
                status VARCHAR(20) DEFAULT 'ACTIVE'
            )
        """);
        jdbcTemplate.execute("DELETE FROM products");

        // Clear previous state respecting FK dependencies
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        bankAccountRepository.deleteAll();
        documentRepository.deleteAll();
        storeRepository.deleteAll();
        sellerRepository.deleteAll();
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure roles & permissions exist
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Default customer role")
                        .build()));

        PermissionEntity catManagePerm = permissionRepository.findByName("CATEGORY_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .name("CATEGORY_MANAGE")
                        .description("Manage categories")
                        .build()));

        PermissionEntity brandManagePerm = permissionRepository.findByName("BRAND_MANAGE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder()
                        .name("BRAND_MANAGE")
                        .description("Manage brands")
                        .build()));

        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator role")
                        .permissions(Set.of(catManagePerm, brandManagePerm))
                        .build()));

        // Register customer
        customerToken = registerAndLogin("Charlie Customer", "charlie@customer.com", "+919111222333", "CustomerPass123!");

        // Register and promote admin
        registerAndLogin("Catalog Admin", "catalog.admin@bazaarhub.com", "+919999888777", "AdminPass123!");
        UserEntity adminUser = userRepository.findByIdentifierWithRolesAndPermissions("catalog.admin@bazaarhub.com").orElseThrow();
        adminUser.getRoles().add(adminRole);
        userRepository.save(adminUser);

        adminToken = login("catalog.admin@bazaarhub.com", "AdminPass123!");
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
    // Category Integration Tests
    // =========================================================================

    @Test
    @DisplayName("End-to-end Category Lifecycle: Create root & child, get tree, update, deactivate, verify security")
    void categoryLifecycle_endToEnd() throws Exception {
        // 1. Admin creates Root Category: Electronics
        CategoryCreateRequest rootReq = CategoryCreateRequest.builder()
                .name("Electronics")
                .slug("electronics")
                .description("All consumer electronic devices")
                .displayOrder(1)
                .build();

        MvcResult rootResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Electronics")))
                .andExpect(jsonPath("$.data.slug", is("electronics")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andReturn();

        Long rootCatId = objectMapper.readTree(rootResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Admin creates Subcategory: Smartphones under Electronics
        CategoryCreateRequest childReq = CategoryCreateRequest.builder()
                .name("Smartphones")
                .slug("smartphones")
                .parentId(rootCatId)
                .description("Latest mobile phones and accessories")
                .displayOrder(1)
                .build();

        MvcResult childResult = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.parentId", is(rootCatId.intValue())))
                .andExpect(jsonPath("$.data.parentName", is("Electronics")))
                .andReturn();

        Long childCatId = objectMapper.readTree(childResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Customer or unauthenticated user browses public category endpoints
        // 3a. Public list
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // 3b. Public root categories
        mockMvc.perform(get("/api/v1/categories/root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(rootCatId.intValue())))
                .andExpect(jsonPath("$.data[0].name", is("Electronics")));

        // 3c. Public category tree
        mockMvc.perform(get("/api/v1/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id", is(rootCatId.intValue())))
                .andExpect(jsonPath("$.data[0].children", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children[0].id", is(childCatId.intValue())))
                .andExpect(jsonPath("$.data[0].children[0].name", is("Smartphones")));

        // 3d. Public category by ID and slug
        mockMvc.perform(get("/api/v1/categories/" + rootCatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Electronics")));

        mockMvc.perform(get("/api/v1/categories/slug/electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(rootCatId.intValue())));

        // 4. Admin updates category details
        CategoryUpdateRequest updateReq = CategoryUpdateRequest.builder()
                .name("Consumer Electronics")
                .slug("consumer-electronics")
                .description("Updated description")
                .displayOrder(5)
                .build();

        mockMvc.perform(put("/api/v1/admin/categories/" + rootCatId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Consumer Electronics")))
                .andExpect(jsonPath("$.data.slug", is("consumer-electronics")));

        // 5. Test circular hierarchy detection: Attempt to make root category a child of its own subcategory
        CategoryUpdateRequest circularReq = CategoryUpdateRequest.builder()
                .name("Consumer Electronics")
                .parentId(childCatId) // Root becomes child of its child!
                .build();

        mockMvc.perform(put("/api/v1/admin/categories/" + rootCatId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(circularReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Circular category relationship detected")));

        // 6. Test self-parenting prevention
        CategoryUpdateRequest selfParentReq = CategoryUpdateRequest.builder()
                .name("Consumer Electronics")
                .parentId(rootCatId)
                .build();

        mockMvc.perform(put("/api/v1/admin/categories/" + rootCatId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfParentReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("A category cannot be its own parent")));

        // 7. Test deactivation guard: Cannot deactivate while active products are linked
        CategoryStatusUpdateRequest deactivateReq = CategoryStatusUpdateRequest.builder()
                .status(CatalogStatus.INACTIVE)
                .build();

        jdbcTemplate.execute("INSERT INTO products (category_id, status) VALUES (" + childCatId + ", 'ACTIVE')");

        mockMvc.perform(patch("/api/v1/admin/categories/" + childCatId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("active product(s) linked to it")));

        // Clear products
        jdbcTemplate.execute("DELETE FROM products");

        // Test deactivation guard: Parent cannot be deactivated while active child subcategory exists
        mockMvc.perform(patch("/api/v1/admin/categories/" + rootCatId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("active subcategory/subcategories")));

        // Deactivate child first
        mockMvc.perform(patch("/api/v1/admin/categories/" + childCatId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        // Now deactivating parent succeeds
        mockMvc.perform(patch("/api/v1/admin/categories/" + rootCatId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        // Deactivated category returns 404 on public endpoint
        mockMvc.perform(get("/api/v1/categories/" + rootCatId))
                .andExpect(status().isNotFound());

        // 8. Security verification: Non-admin (customer) forbidden from admin operations
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Brand Integration Tests
    // =========================================================================

    @Test
    @DisplayName("End-to-end Brand Lifecycle: Create, conflict detection, public browse, update, status toggle, security")
    void brandLifecycle_endToEnd() throws Exception {
        // 1. Admin creates Brand: Apple
        BrandCreateRequest brandReq = BrandCreateRequest.builder()
                .name("Apple")
                .slug("apple")
                .logoUrl("https://cdn.bazaarhub.com/brands/apple.png")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Apple")))
                .andExpect(jsonPath("$.data.slug", is("apple")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andReturn();

        Long brandId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Conflict: Duplicate brand name
        BrandCreateRequest dupNameReq = BrandCreateRequest.builder()
                .name("Apple")
                .slug("apple-2")
                .build();

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupNameReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // 3. Conflict: Duplicate slug
        BrandCreateRequest dupSlugReq = BrandCreateRequest.builder()
                .name("Apple Inc.")
                .slug("apple")
                .build();

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupSlugReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already in use")));

        // 4. Public access to active brand
        mockMvc.perform(get("/api/v1/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Apple")));

        mockMvc.perform(get("/api/v1/brands/" + brandId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Apple")));

        mockMvc.perform(get("/api/v1/brands/slug/apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(brandId.intValue())));

        mockMvc.perform(get("/api/v1/brands/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 5. Admin updates brand
        BrandUpdateRequest updateReq = BrandUpdateRequest.builder()
                .name("Apple Global")
                .slug("apple-global")
                .logoUrl("https://cdn.bazaarhub.com/brands/apple-global.png")
                .build();

        mockMvc.perform(put("/api/v1/admin/brands/" + brandId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Apple Global")))
                .andExpect(jsonPath("$.data.slug", is("apple-global")));

        // 6. Test deactivation guard: Cannot deactivate brand when active products are linked
        BrandStatusUpdateRequest deactivateReq = BrandStatusUpdateRequest.builder()
                .status(CatalogStatus.INACTIVE)
                .build();

        jdbcTemplate.execute("INSERT INTO products (brand_id, status) VALUES (" + brandId + ", 'ACTIVE')");

        mockMvc.perform(patch("/api/v1/admin/brands/" + brandId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("active product(s) linked to it")));

        // Clear products
        jdbcTemplate.execute("DELETE FROM products");

        // Admin deactivates brand
        mockMvc.perform(patch("/api/v1/admin/brands/" + brandId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deactivateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("INACTIVE")));

        // Public lookup of deactivated brand returns 404
        mockMvc.perform(get("/api/v1/brands/" + brandId))
                .andExpect(status().isNotFound());

        // 7. Security: Customer cannot perform administrative operations
        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(brandReq)))
                .andExpect(status().isForbidden());
    }
}
