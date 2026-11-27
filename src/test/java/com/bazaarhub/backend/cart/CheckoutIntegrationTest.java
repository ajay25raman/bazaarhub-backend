package com.bazaarhub.backend.cart;

import com.bazaarhub.backend.cart.application.dto.request.AddToCartRequest;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutAddressRequest;
import com.bazaarhub.backend.cart.application.dto.request.CheckoutValidateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutIntegrationTest {

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

    @Autowired
    private SpringDataAddressRepository addressRepository;

    private String customer1Token;
    private String customer2Token;
    private UserEntity customer1;
    private UserEntity customer2;

    private ProductEntity productA;
    private ProductVariantEntity variantA;
    private InventoryEntity inventoryA;
    private AddressEntity customer1Address;
    private AddressEntity customer2Address;

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

        // 1. Roles and permissions
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

        // 2. Customers
        customer1Token = registerAndLogin("alice@checkout.com", "Password@123", "Alice Checkout", "+919876510001");
        customer1 = userRepository.findByEmail("alice@checkout.com").orElseThrow();

        customer2Token = registerAndLogin("bob@checkout.com", "Password@123", "Bob Checkout", "+919876510002");
        customer2 = userRepository.findByEmail("bob@checkout.com").orElseThrow();

        // 3. Addresses
        customer1Address = addressRepository.save(AddressEntity.builder()
                .user(customer1)
                .recipientName("Alice Checkout")
                .phone("+919876510001")
                .addressLine1("100 Innovation Way, ITPL")
                .city("Bangalore")
                .state("Karnataka")
                .country("India")
                .postalCode("560066")
                .addressType("HOME")
                .isDefault(true)
                .build());

        customer2Address = addressRepository.save(AddressEntity.builder()
                .user(customer2)
                .recipientName("Bob Checkout")
                .phone("+919876510002")
                .addressLine1("200 Cyber Hub")
                .city("Gurgaon")
                .state("Haryana")
                .country("India")
                .postalCode("122002")
                .addressType("WORK")
                .isDefault(true)
                .build());

        // 4. Category & Seller
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Appliances")
                .slug("appliances")
                .status(CatalogStatus.ACTIVE)
                .build());

        UserEntity sellerUser = userRepository.save(UserEntity.builder()
                .email("seller@checkout.com")
                .passwordHash("hashed")
                .fullName("Seller Merchant")
                .phone("+919876510099")
                .accountStatus(com.bazaarhub.backend.identity.domain.model.AccountStatus.ACTIVE)
                .build());

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Prime Home Store")
                .businessEmail("prime@homestore.com")
                .businessPhone("+919876510010")
                .taxIdentifier("GSTIN33333333")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        // 5. Product & Variant
        productA = productRepository.save(ProductEntity.builder()
                .name("Smart Air Purifier Max")
                .slug("smart-air-purifier-max")
                .category(category)
                .seller(seller)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        variantA = variantRepository.save(ProductVariantEntity.builder()
                .product(productA)
                .sku("AIR-PUR-MAX-01")
                .variantName("Pure White HEPA H13")
                .sellingPrice(new BigDecimal("12999.00"))
                .mrp(new BigDecimal("15999.00"))
                .taxRate(new BigDecimal("18.00"))
                .status(CatalogStatus.ACTIVE)
                .build());

        inventoryA = inventoryRepository.save(InventoryEntity.builder()
                .variant(variantA)
                .totalQuantity(20)
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
    // 1. CHECKOUT SUMMARY TESTS
    // =========================================================================

    @Test
    @DisplayName("Scenario 20: Checkout summary with empty cart returns canProceed=false and EMPTY_CART")
    void testCheckoutSummary_EmptyCart() throws Exception {
        mockMvc.perform(get("/api/v1/checkout/summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(false))
                .andExpect(jsonPath("$.data.checkoutStatus").value("EMPTY_CART"))
                .andExpect(jsonPath("$.data.itemsCount").value(0))
                .andExpect(jsonPath("$.data.validationErrors[0]", containsString("Cart is empty")));
    }

    @Test
    @DisplayName("Scenario 21: Checkout summary with valid cart and fallback default address is READY")
    void testCheckoutSummary_ValidCart_DefaultAddress() throws Exception {
        // Add 2 items to customer 1's cart
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 2))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/checkout/summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(true))
                .andExpect(jsonPath("$.data.checkoutStatus").value("READY"))
                .andExpect(jsonPath("$.data.itemsCount").value(2))
                .andExpect(jsonPath("$.data.subtotal").value(25998.00))
                .andExpect(jsonPath("$.data.taxAmount").value(4679.64))
                .andExpect(jsonPath("$.data.grandTotal").value(30677.64))
                .andExpect(jsonPath("$.data.selectedAddress.id").value(customer1Address.getId()))
                .andExpect(jsonPath("$.data.selectedAddress.recipientName").value("Alice Checkout"))
                .andExpect(jsonPath("$.data.validationErrors").isEmpty());
    }

    @Test
    @DisplayName("Scenario 22: Checkout summary with explicit address ID sets selected address")
    void testCheckoutSummary_ValidCart_ExplicitAddress() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/checkout/summary?addressId=" + customer1Address.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(true))
                .andExpect(jsonPath("$.data.selectedAddress.id").value(customer1Address.getId()));
    }

    @Test
    @DisplayName("Scenario 23: Checkout summary when product becomes unavailable blocks checkout")
    void testCheckoutSummary_UnavailableProduct_BlocksCheckout() throws Exception {
        // Add to cart while active
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        // Deactivate product
        productA.setStatus(ProductStatus.INACTIVE);
        productRepository.save(productA);

        mockMvc.perform(get("/api/v1/checkout/summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(false))
                .andExpect(jsonPath("$.data.checkoutStatus").value("ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.validationErrors", hasItem(containsString("inactive"))));
    }

    @Test
    @DisplayName("Scenario 24: Checkout summary when stock drops below cart quantity blocks checkout")
    void testCheckoutSummary_InsufficientStock_BlocksCheckout() throws Exception {
        // Add 5 items to cart
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 5))))
                .andExpect(status().isCreated());

        // Update warehouse inventory reserved quantity so available stock drops to 2
        inventoryA.setReservedQuantity(18); // total 20 - reserved 18 = available 2
        inventoryRepository.save(inventoryA);

        mockMvc.perform(get("/api/v1/checkout/summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(false))
                .andExpect(jsonPath("$.data.checkoutStatus").value("ACTION_REQUIRED"))
                .andExpect(jsonPath("$.data.validationErrors", hasItem(containsString("insufficient stock"))));
    }

    @Test
    @DisplayName("Scenario 25: Checkout summary with another customer's address rejected with 403 Forbidden")
    void testCheckoutSummary_AnotherCustomersAddress_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        // Customer 1 requests checkout summary with Customer 2's address
        mockMvc.perform(get("/api/v1/checkout/summary?addressId=" + customer2Address.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 26: Checkout summary with non-existent address rejected with 404 Not Found")
    void testCheckoutSummary_NonExistentAddress_Returns404() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/checkout/summary?addressId=999999")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // 2. CHECKOUT VALIDATION & ADDRESS SELECTION
    // =========================================================================

    @Test
    @DisplayName("Scenario 27: Validate checkout returns valid=true when cart items and address are valid")
    void testValidateCheckout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        CheckoutValidateRequest request = new CheckoutValidateRequest(customer1Address.getId());

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.addressId").value(customer1Address.getId()))
                .andExpect(jsonPath("$.data.errors").isEmpty());
    }

    @Test
    @DisplayName("Scenario 28: Validate checkout on empty cart returns 422 Unprocessable Content")
    void testValidateCheckout_EmptyCart_Returns422() throws Exception {
        CheckoutValidateRequest request = new CheckoutValidateRequest(customer1Address.getId());

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Cart is empty")));
    }

    @Test
    @DisplayName("Scenario 29: Select checkout address successfully updates delivery address in summary")
    void testSelectCheckoutAddress_Success() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(null, variantA.getId(), 1))))
                .andExpect(status().isCreated());

        CheckoutAddressRequest request = new CheckoutAddressRequest(customer1Address.getId());

        mockMvc.perform(post("/api/v1/checkout/address")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedAddress.id").value(customer1Address.getId()))
                .andExpect(jsonPath("$.data.canProceedToCheckout").value(true));
    }

    @Test
    @DisplayName("Scenario 30: Select checkout address belonging to another customer rejected with 403 Forbidden")
    void testSelectCheckoutAddress_AnotherCustomer_Returns403() throws Exception {
        CheckoutAddressRequest request = new CheckoutAddressRequest(customer2Address.getId());

        mockMvc.perform(post("/api/v1/checkout/address")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Scenario 31: Unauthenticated checkout request rejected with 401 Unauthorized")
    void testCheckoutUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/checkout/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/checkout/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1}"))
                .andExpect(status().isUnauthorized());
    }
}
