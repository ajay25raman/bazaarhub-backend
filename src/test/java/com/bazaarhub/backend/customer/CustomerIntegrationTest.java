package com.bazaarhub.backend.customer;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.customer.application.dto.request.*;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataCustomerProfileRepository;
import com.bazaarhub.backend.identity.application.dto.LoginRequest;
import com.bazaarhub.backend.identity.application.dto.RegisterRequest;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.PermissionEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataPermissionRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserDeviceRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.wishlist.application.dto.request.AddWishlistItemRequest;
import com.bazaarhub.backend.wishlist.application.dto.request.WishlistUpdateRequest;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistItemRepository;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerIntegrationTest {

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
    private SpringDataCustomerProfileRepository profileRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataWishlistRepository wishlistRepository;

    @Autowired
    private SpringDataWishlistItemRepository wishlistItemRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductVariantRepository variantRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataInventoryRepository inventoryRepository;

    private String customer1Token;
    private String customer2Token;
    private Long customer1Id;
    private Long customer2Id;
    private Long activeProductId;

    @BeforeEach
    void setUp() throws Exception {
        // Clean all dependent tables
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

        // Ensure permissions exist
        PermissionEntity profileRead = permissionRepository.findByName("CUSTOMER_PROFILE_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("CUSTOMER_PROFILE_READ").description("Read profile").build()));
        PermissionEntity profileUpdate = permissionRepository.findByName("CUSTOMER_PROFILE_UPDATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("CUSTOMER_PROFILE_UPDATE").description("Update profile").build()));
        PermissionEntity wishlistRead = permissionRepository.findByName("WISHLIST_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("WISHLIST_READ").description("Read wishlist").build()));
        PermissionEntity wishlistUpdate = permissionRepository.findByName("WISHLIST_UPDATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("WISHLIST_UPDATE").description("Update wishlist").build()));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .permissions(Set.of(profileRead, profileUpdate, wishlistRead, wishlistUpdate))
                        .build()));

        // Register Customer 1
        customer1Token = registerAndLogin("alice@customer.com", "Password@123", "Alice Johnson", "+919876543210");
        customer1Id = userRepository.findByEmail("alice@customer.com").orElseThrow().getId();

        // Register Customer 2
        customer2Token = registerAndLogin("bob@customer.com", "Password@123", "Bob Smith", "+919876543211");
        customer2Id = userRepository.findByEmail("bob@customer.com").orElseThrow().getId();

        // Create category and active product
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Smartphones")
                .slug("smartphones")
                .status(CatalogStatus.ACTIVE)
                .build());

        // Create a dummy seller
        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(userRepository.findByEmail("bob@customer.com").orElseThrow())
                .businessName("Acme Retailers")
                .businessEmail("acme@retail.com")
                .businessPhone("+919876543211")
                .taxIdentifier("GSTIN12345678")
                .build());

        ProductEntity product = productRepository.save(ProductEntity.builder()
                .name("Flagship Phone Ultra")
                .slug("flagship-phone-ultra")
                .category(category)
                .seller(seller)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());
        activeProductId = product.getId();

        ProductVariantEntity variant = variantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .sku("ULTRA-BLK-256")
                .variantName("Black 256GB")
                .sellingPrice(new BigDecimal("79999.00"))
                .mrp(new BigDecimal("89999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant)
                .totalQuantity(25)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());
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
    // 1. CUSTOMER PROFILE TESTS
    // =========================================================================

    @Test
    @DisplayName("Create customer profile successfully")
    void testCreateCustomerProfileSuccess() throws Exception {
        CustomerProfileCreateRequest request = CustomerProfileCreateRequest.builder()
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("FEMALE")
                .profileImageUrl("https://images.bazaarhub.com/profiles/alice.jpg")
                .build();

        mockMvc.perform(post("/api/v1/customer/profile")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-05-15"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://images.bazaarhub.com/profiles/alice.jpg"));
    }

    @Test
    @DisplayName("Duplicate customer profile creation returns 409 Conflict")
    void testCreateCustomerProfileDuplicateThrows409() throws Exception {
        CustomerProfileCreateRequest request = CustomerProfileCreateRequest.builder()
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("FEMALE")
                .build();

        // 1. First creation
        mockMvc.perform(post("/api/v1/customer/profile")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Duplicate creation
        mockMvc.perform(post("/api/v1/customer/profile")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Get customer profile returns 404 when profile not yet created")
    void testGetProfileNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/customer/profile")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Get account profile returns basic user details even if profile not created")
    void testGetAccountProfileWithoutCreatedProfile() throws Exception {
        mockMvc.perform(get("/api/v1/customer/profile/account")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.email").value("alice@customer.com"));
    }

    @Test
    @DisplayName("Update customer profile updates fields and full name")
    void testUpdateCustomerProfile() throws Exception {
        CustomerProfileUpdateRequest request = CustomerProfileUpdateRequest.builder()
                .fullName("Alice B. Johnson")
                .dateOfBirth(LocalDate.of(1994, 8, 20))
                .gender("FEMALE")
                .profileImageUrl("https://images.bazaarhub.com/profiles/alice_updated.jpg")
                .build();

        mockMvc.perform(put("/api/v1/customer/profile")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Alice B. Johnson"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1994-08-20"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://images.bazaarhub.com/profiles/alice_updated.jpg"));
    }

    @Test
    @DisplayName("Patch date of birth, gender, and profile image separately")
    void testPatchCustomerProfileGranularFields() throws Exception {
        // 1. Patch DOB
        UpdateDobRequest dobRequest = UpdateDobRequest.builder()
                .dateOfBirth(LocalDate.of(1992, 10, 10))
                .build();
        mockMvc.perform(patch("/api/v1/customer/profile/dob")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dateOfBirth").value("1992-10-10"));

        // 2. Patch Gender
        UpdateGenderRequest genderRequest = UpdateGenderRequest.builder()
                .gender("OTHER")
                .build();
        mockMvc.perform(patch("/api/v1/customer/profile/gender")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genderRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gender").value("OTHER"));

        // 3. Patch Image URL
        UpdateProfileImageRequest imageRequest = UpdateProfileImageRequest.builder()
                .profileImageUrl("https://images.bazaarhub.com/profiles/avatar.png")
                .build();
        mockMvc.perform(patch("/api/v1/customer/profile/image")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://images.bazaarhub.com/profiles/avatar.png"));
    }

    // =========================================================================
    // 2. CUSTOMER ADDRESS TESTS
    // =========================================================================

    @Test
    @DisplayName("Add address and verify first address automatically becomes default")
    void testAddAddressFirstBecomesDefault() throws Exception {
        AddressCreateRequest request = AddressCreateRequest.builder()
                .addressType("HOME")
                .recipientName("Alice Johnson")
                .phone("+919876543210")
                .addressLine1("Flat 402, Lotus Heights")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .isDefault(false) // First address should automatically be promoted to default
                .build();

        mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipientName").value("Alice Johnson"))
                .andExpect(jsonPath("$.data.city").value("Bengaluru"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("Add second address with isDefault=true unsets previous default")
    void testAddSecondAddressWithDefaultTrue() throws Exception {
        // Address 1
        AddressCreateRequest req1 = AddressCreateRequest.builder()
                .recipientName("Alice Home")
                .phone("+919876543210")
                .addressLine1("Home Address 1")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .build();
        MvcResult res1 = mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addr1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Address 2 with isDefault = true
        AddressCreateRequest req2 = AddressCreateRequest.builder()
                .recipientName("Alice Office")
                .phone("+919876543210")
                .addressLine1("Tech Park Office")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560100")
                .isDefault(true)
                .build();
        mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(true));

        // Verify Address 1 is no longer default
        mockMvc.perform(get("/api/v1/customer/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(false));
    }

    @Test
    @DisplayName("Customer cannot access another customer's address (403 Forbidden)")
    void testCrossCustomerAddressAccessForbidden() throws Exception {
        // Customer 1 adds address
        AddressCreateRequest req1 = AddressCreateRequest.builder()
                .recipientName("Alice Private")
                .phone("+919876543210")
                .addressLine1("Private Lane")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .build();
        MvcResult res1 = mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addr1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asLong();

        // Customer 2 attempts to get Customer 1's address
        mockMvc.perform(get("/api/v1/customer/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        // Customer 2 attempts to delete Customer 1's address
        mockMvc.perform(delete("/api/v1/customer/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Delete default address automatically promotes remaining address to default")
    void testDeleteDefaultAddressPromotesRemaining() throws Exception {
        // 1. Add Address 1 (becomes default)
        AddressCreateRequest req1 = AddressCreateRequest.builder()
                .recipientName("Address One")
                .phone("+919876543210")
                .addressLine1("Street 1")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .build();
        MvcResult res1 = mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addr1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 2. Add Address 2 (non-default)
        AddressCreateRequest req2 = AddressCreateRequest.builder()
                .recipientName("Address Two")
                .phone("+919876543210")
                .addressLine1("Street 2")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560002")
                .isDefault(false)
                .build();
        MvcResult res2 = mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        Long addr2Id = objectMapper.readTree(res2.getResponse().getContentAsString()).path("data").path("id").asLong();

        // 3. Delete Address 1 (which was default)
        mockMvc.perform(delete("/api/v1/customer/addresses/" + addr1Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // 4. Verify Address 2 is now promoted to default
        mockMvc.perform(get("/api/v1/customer/addresses/" + addr2Id)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    @DisplayName("Get default address returns default address")
    void testGetDefaultAddress() throws Exception {
        AddressCreateRequest req = AddressCreateRequest.builder()
                .recipientName("Alice Main")
                .phone("+919876543210")
                .addressLine1("Main Street")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .build();
        mockMvc.perform(post("/api/v1/customer/addresses")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/customer/addresses/default")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value("Alice Main"))
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    // =========================================================================
    // 3. CUSTOMER WISHLIST TESTS
    // =========================================================================

    @Test
    @DisplayName("Get wishlist auto-creates default wishlist")
    void testGetWishlistAutoCreates() throws Exception {
        mockMvc.perform(get("/api/v1/customer/wishlist")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("My Wishlist"))
                .andExpect(jsonPath("$.data.totalItems").value(0));
    }

    @Test
    @DisplayName("Rename wishlist updates display name")
    void testRenameWishlist() throws Exception {
        WishlistUpdateRequest request = WishlistUpdateRequest.builder()
                .name("Holiday Wishlist 2026")
                .build();

        mockMvc.perform(put("/api/v1/customer/wishlist")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Holiday Wishlist 2026"));
    }

    @Test
    @DisplayName("Add product to wishlist and list items with stock status")
    void testAddProductToWishlistAndListItems() throws Exception {
        AddWishlistItemRequest request = AddWishlistItemRequest.builder()
                .productId(activeProductId)
                .build();

        // 1. Add product to wishlist
        mockMvc.perform(post("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productId").value(activeProductId))
                .andExpect(jsonPath("$.data.productName").value("Flagship Phone Ultra"))
                .andExpect(jsonPath("$.data.inStock").value(true));

        // 2. Check if product exists in wishlist
        mockMvc.perform(get("/api/v1/customer/wishlist/items/" + activeProductId + "/exists")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inWishlist").value(true));

        // 3. List items
        mockMvc.perform(get("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productId").value(activeProductId));

        // 4. Duplicate add returns 409 Conflict
        mockMvc.perform(post("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        // 5. Remove product from wishlist
        mockMvc.perform(delete("/api/v1/customer/wishlist/items/" + activeProductId)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // 6. Verify removed
        mockMvc.perform(get("/api/v1/customer/wishlist/items/" + activeProductId + "/exists")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inWishlist").value(false));
    }

    @Test
    @DisplayName("Clear wishlist removes all saved products")
    void testClearWishlist() throws Exception {
        AddWishlistItemRequest request = AddWishlistItemRequest.builder()
                .productId(activeProductId)
                .build();

        mockMvc.perform(post("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/wishlist/items")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // =========================================================================
    // 4. SECURITY & UNTHORIZED ACCESS TESTS
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated request to customer endpoints returns 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/customer/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customer/addresses"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customer/wishlist"))
                .andExpect(status().isUnauthorized());
    }
}
