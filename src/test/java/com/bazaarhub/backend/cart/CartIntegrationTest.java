package com.bazaarhub.backend.cart;

import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.application.dto.request.UpdateCartItemQuantityRequest;
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
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartIntegrationTest {

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
    private SpringDataCartRepository cartRepository;

    @Autowired
    private SpringDataCartItemRepository cartItemRepository;

    private String customer1Token;
    private String customer2Token;
    private Long customer1Id;
    private Long customer2Id;

    private ProductVariantEntity variant1SellerA;
    private ProductVariantEntity variant2SellerA;
    private ProductVariantEntity variantSellerB;
    private ProductVariantEntity outOfStockVariant;
    private ProductVariantEntity inactiveVariant;
    private ProductEntity inactiveProduct;
    private ProductVariantEntity variantInactiveProduct;

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

        // 1. Ensure permissions and customer role exist
        PermissionEntity cartRead = permissionRepository.findByName("CART_READ")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("CART_READ").description("Read cart").build()));
        PermissionEntity cartUpdate = permissionRepository.findByName("CART_UPDATE")
                .orElseGet(() -> permissionRepository.save(PermissionEntity.builder().name("CART_UPDATE").description("Update cart").build()));

        roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .name("ROLE_CUSTOMER")
                        .description("Customer")
                        .permissions(Set.of(cartRead, cartUpdate))
                        .build()));

        // 2. Register Customers
        customer1Token = registerAndLogin("alice@cart.com", "Password@123", "Alice Cart", "+919876500001");
        customer1Id = userRepository.findByEmail("alice@cart.com").orElseThrow().getId();

        customer2Token = registerAndLogin("bob@cart.com", "Password@123", "Bob Cart", "+919876500002");
        customer2Id = userRepository.findByEmail("bob@cart.com").orElseThrow().getId();

        // 3. Category
        CategoryEntity electronics = categoryRepository.save(CategoryEntity.builder()
                .name("Electronics")
                .slug("electronics")
                .status(CatalogStatus.ACTIVE)
                .build());

        // 4. Sellers
        UserEntity sellerUserA = userRepository.save(UserEntity.builder()
                .email("sellera@cart.com")
                .passwordHash("hashed")
                .fullName("Seller A Merchant")
                .phone("+919876500010")
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        UserEntity sellerUserB = userRepository.save(UserEntity.builder()
                .email("sellerb@cart.com")
                .passwordHash("hashed")
                .fullName("Seller B Merchant")
                .phone("+919876500020")
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        SellerEntity sellerA = sellerRepository.save(SellerEntity.builder()
                .user(sellerUserA)
                .businessName("Seller Alpha Electronics")
                .businessEmail("alpha@sellers.com")
                .businessPhone("+919876500010")
                .taxIdentifier("GSTIN11111111")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        SellerEntity sellerB = sellerRepository.save(SellerEntity.builder()
                .user(sellerUserB)
                .businessName("Seller Beta Audio")
                .businessEmail("beta@sellers.com")
                .businessPhone("+919876500020")
                .taxIdentifier("GSTIN22222222")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        // 5. Products
        ProductEntity productA = productRepository.save(ProductEntity.builder()
                .name("Alpha Phone Ultra")
                .slug("alpha-phone-ultra")
                .category(electronics)
                .seller(sellerA)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        ProductEntity productB = productRepository.save(ProductEntity.builder()
                .name("Beta Wireless Headphones")
                .slug("beta-wireless-headphones")
                .category(electronics)
                .seller(sellerB)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        inactiveProduct = productRepository.save(ProductEntity.builder()
                .name("Inactive Tablet Pro")
                .slug("inactive-tablet-pro")
                .category(electronics)
                .seller(sellerA)
                .status(ProductStatus.INACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        // 6. Variants
        variant1SellerA = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-PHONE-128")
                .variantName("Midnight Blue 128GB")
                .sellingPrice(new BigDecimal("49999.00"))
                .mrp(new BigDecimal("54999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        variant2SellerA = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-PHONE-256")
                .variantName("Cosmic Silver 256GB")
                .sellingPrice(new BigDecimal("59999.00"))
                .mrp(new BigDecimal("64999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        variantSellerB = variantRepository.save(ProductVariantEntity.builder()
                .product(productB)
                .sku("BETA-HEADPHONES-BLK")
                .variantName("Black Bass Edition")
                .sellingPrice(new BigDecimal("4999.00"))
                .mrp(new BigDecimal("6999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        outOfStockVariant = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-PHONE-512-OOS")
                .variantName("512GB Out of Stock")
                .sellingPrice(new BigDecimal("69999.00"))
                .mrp(new BigDecimal("74999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        inactiveVariant = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("ALPHA-PHONE-INACTIVE")
                .variantName("Discontinued Gold")
                .sellingPrice(new BigDecimal("49999.00"))
                .mrp(new BigDecimal("54999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.INACTIVE)
                .build());

        variantInactiveProduct = variantRepository.save(ProductVariantEntity.builder()
                .product(inactiveProduct)
                .sku("INACTIVE-TAB-128")
                .variantName("Grey 128GB")
                .sellingPrice(new BigDecimal("29999.00"))
                .mrp(new BigDecimal("34999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        // 7. Inventories
        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant1SellerA)
                .totalQuantity(30)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant2SellerA)
                .totalQuantity(15)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variantSellerB)
                .totalQuantity(25)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(outOfStockVariant)
                .totalQuantity(5)
                .reservedQuantity(5) // available = 0
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(inactiveVariant)
                .totalQuantity(10)
                .reservedQuantity(0)
                .safetyStock(2)
                .build());

        inventoryRepository.save(InventoryEntity.builder()
                .variant(variantInactiveProduct)
                .totalQuantity(10)
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
    // 1. GET CART & AUTO-CREATION
    // =========================================================================

    @Test
    @DisplayName("Scenario 1 & 2: Get empty customer cart - auto-creates active cart with 0 items")
    void testGetEmptyCart_AutoCreatesCart() throws Exception {
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0.00))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.sellers").isEmpty());
    }

    // =========================================================================
    // 2. ADD TO CART
    // =========================================================================

    @Test
    @DisplayName("Scenario 3: Add single product variant to cart successfully")
    void testAddToCart_SingleItem_Success() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(99998.00))
                .andExpect(jsonPath("$.data.items[0].variantId").value(variant1SellerA.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(49999.00))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(99998.00))
                .andExpect(jsonPath("$.data.items[0].inStock").value(true))
                .andExpect(jsonPath("$.data.items[0].purchasable").value(true))
                .andExpect(jsonPath("$.data.sellers[0].businessName").value("Seller Alpha Electronics"))
                .andExpect(jsonPath("$.data.sellers[0].itemCount").value(2))
                .andExpect(jsonPath("$.data.sellers[0].subtotal").value(99998.00));
    }

    @Test
    @DisplayName("Scenario 4: Add same variant again increments quantity without duplicate row")
    void testAddToCart_SameVariant_IncrementsQuantity() throws Exception {
        AddToCartRequest firstAdd = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstAdd)))
                .andExpect(status().isCreated());

        AddToCartRequest secondAdd = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(3)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondAdd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalItems").value(5))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(249995.00));

        // Verify only 1 row exists in cart_items database
        assertEquals(1, cartItemRepository.count());
    }

    @Test
    @DisplayName("Scenario 5: Add different variants creates distinct cart item rows")
    void testAddToCart_DifferentVariants_CreatesDistinctRows() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(variant1SellerA.getProduct().getId(), variant1SellerA.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(variant2SellerA.getProduct().getId(), variant2SellerA.getId(), 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(169997.00)); // 49999 + 2*59999 = 169997

        assertEquals(2, cartItemRepository.count());
    }

    @Test
    @DisplayName("Scenario 6: Multi-vendor cart correctly groups items and subtotals by seller")
    void testAddToCart_MultiVendor_GroupsSellers() throws Exception {
        // Add item from Seller Alpha
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variant1SellerA.getId(), 1))))
                .andExpect(status().isCreated());

        // Add item from Seller Beta
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantSellerB.getId(), 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(59997.00)) // 49999 + 2*4999 = 59997
                .andExpect(jsonPath("$.data.sellers", hasSize(2)));
    }

    @Test
    @DisplayName("Scenario 7: Add inactive product rejected with 400 Bad Request")
    void testAddToCart_InactiveProduct_Fails() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variantInactiveProduct.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 8: Add unavailable product (out of stock) rejected with 422 Unprocessable Content")
    void testAddToCart_OutOfStock_Fails() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(outOfStockVariant.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("out of stock")));
    }

    @Test
    @DisplayName("Scenario 9: Add inactive variant rejected with 400 Bad Request")
    void testAddToCart_InactiveVariant_Fails() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(inactiveVariant.getId())
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("inactive")));
    }

    @Test
    @DisplayName("Scenario 10: Add invalid quantity (<= 0) rejected with 400 Bad Request")
    void testAddToCart_InvalidQuantity_Fails() throws Exception {
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 11: Add quantity exceeding available stock rejected with 422")
    void testAddToCart_QuantityExceedingStock_Fails() throws Exception {
        // available stock is 30
        AddToCartRequest request = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(35)
                .build();

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("exceeds available stock")));
    }

    // =========================================================================
    // 3. UPDATE QUANTITY & REMOVE ITEMS
    // =========================================================================

    @Test
    @DisplayName("Scenario 12: Update cart item quantity recalculates subtotals")
    void testUpdateCartItemQuantity_Success() throws Exception {
        AddToCartRequest addRequest = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(1)
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(addResult.getResponse().getContentAsString());
        long itemId = root.path("data").path("items").get(0).path("itemId").asLong();

        UpdateCartItemQuantityRequest updateRequest = new UpdateCartItemQuantityRequest(3);

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(149997.00))
                .andExpect(jsonPath("$.data.subtotal").value(149997.00));
    }

    @Test
    @DisplayName("Scenario 13: Update quantity exceeding stock rejected with 422")
    void testUpdateCartItemQuantity_ExceedsStock_Fails() throws Exception {
        AddToCartRequest addRequest = AddToCartRequest.builder()
                .variantId(variant1SellerA.getId())
                .quantity(2)
                .build();

        MvcResult addResult = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("itemId").asLong();

        UpdateCartItemQuantityRequest updateRequest = new UpdateCartItemQuantityRequest(100);

        mockMvc.perform(put("/api/v1/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 14: Remove cart item removes item and recalculates totals")
    void testRemoveCartItem_Success() throws Exception {
        // Add two items
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variant1SellerA.getId(), 1))))
                .andExpect(status().isCreated());

        MvcResult addResult2 = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantSellerB.getId(), 2))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(addResult2.getResponse().getContentAsString());
        long itemToRemoveId = root.path("data").path("items").get(1).path("itemId").asLong();

        mockMvc.perform(delete("/api/v1/cart/items/" + itemToRemoveId)
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(1))
                .andExpect(jsonPath("$.data.subtotal").value(49999.00));
    }

    @Test
    @DisplayName("Scenario 15: Attempt to remove another customer's cart item rejected with 403 Forbidden")
    void testRemoveCartItem_AnotherCustomer_Forbidden() throws Exception {
        // Customer 1 adds item
        MvcResult addResult = mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variant1SellerA.getId(), 1))))
                .andExpect(status().isCreated())
                .andReturn();

        long itemOfCustomer1 = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("itemId").asLong();

        // Customer 2 attempts to delete Customer 1's item
        mockMvc.perform(delete("/api/v1/cart/items/" + itemOfCustomer1)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 16: Clear cart empties all items and resets totals to zero")
    void testClearCart_Success() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variant1SellerA.getId(), 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/cart")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(0))
                .andExpect(jsonPath("$.data.subtotal").value(0.00))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.sellers").isEmpty());

        assertEquals(0, cartItemRepository.count());
    }

    // =========================================================================
    // 4. CART SUMMARY & AUTHORIZATION
    // =========================================================================

    @Test
    @DisplayName("Scenario 17 & 18: Get cart summary returns accurate totals and seller breakdown")
    void testGetCartSummary_Success() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variant1SellerA.getId(), 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantSellerB.getId(), 3))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/cart/summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(5))
                .andExpect(jsonPath("$.data.totalUniqueItems").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(114995.00)) // 2*49999 + 3*4999 = 99998 + 14997 = 114995
                .andExpect(jsonPath("$.data.sellers", hasSize(2)));
    }

    @Test
    @DisplayName("Scenario 25: Unauthenticated access to cart rejected with 401 Unauthorized")
    void testCartUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":1,\"quantity\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
