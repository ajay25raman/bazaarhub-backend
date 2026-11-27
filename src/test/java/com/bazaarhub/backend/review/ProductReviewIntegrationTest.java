package com.bazaarhub.backend.review;

import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
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
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.review.application.dto.request.CreateReviewRequest;
import com.bazaarhub.backend.review.application.dto.request.ModerateReviewRequest;
import com.bazaarhub.backend.review.application.dto.request.UpdateReviewRequest;
import com.bazaarhub.backend.review.domain.model.ReviewStatus;
import com.bazaarhub.backend.review.infrastructure.persistence.repository.SpringDataProductReviewRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductReviewIntegrationTest {

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
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataBrandRepository brandRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductVariantRepository variantRepository;

    @Autowired
    private SpringDataAddressRepository addressRepository;

    @Autowired
    private SpringDataOrderRepository orderRepository;

    @Autowired
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Autowired
    private SpringDataOrderItemRepository orderItemRepository;

    @Autowired
    private SpringDataProductReviewRepository reviewRepository;

    private String customerToken;
    private String secondCustomerToken;
    private String sellerToken;
    private String adminToken;

    private UserEntity customerUser;
    private UserEntity secondCustomerUser;
    private UserEntity sellerUser;
    private UserEntity adminUser;

    private ProductEntity product;
    private ProductVariantEntity variant;

    @BeforeEach
    void setUp() throws Exception {
        cleanDatabase();

        // 1. Ensure Roles
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_CUSTOMER").description("Customer").build()));
        RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_SELLER").description("Seller").build()));
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder().name("ROLE_ADMIN").description("Admin").build()));

        // 2. Register Eligible Customer
        customerToken = registerAndLogin("review.customer@bazaarhub.com", "Password@123", "Customer Ajay", "+919876541001");
        customerUser = userRepository.findByEmail("review.customer@bazaarhub.com").orElseThrow();

        // 3. Register Second Customer (Ineligible - no orders)
        secondCustomerToken = registerAndLogin("review.customer2@bazaarhub.com", "Password@123", "Customer Bob", "+919876541002");
        secondCustomerUser = userRepository.findByEmail("review.customer2@bazaarhub.com").orElseThrow();

        // 4. Register Seller
        registerAndLogin("review.seller@bazaarhub.com", "Password@123", "Merchant Suresh", "+919876541003");
        sellerUser = userRepository.findByEmail("review.seller@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", sellerUser.getId(), sellerRole.getId());
        sellerToken = login("review.seller@bazaarhub.com", "Password@123");

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser)
                .businessName("Bazaar Smart Audio Store")
                .businessEmail("review.seller@bazaarhub.com")
                .businessPhone("+919876541003")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        storeRepository.save(StoreEntity.builder()
                .seller(seller)
                .storeName("Bazaar Smart Audio Store")
                .slug("bazaar-smart-audio-" + UUID.randomUUID().toString().substring(0, 8))
                .status(StoreStatus.ACTIVE)
                .build());

        // 5. Register Admin
        registerAndLogin("review.admin@bazaarhub.com", "Password@123", "Admin Officer", "+919876541004");
        adminUser = userRepository.findByEmail("review.admin@bazaarhub.com").orElseThrow();
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", adminUser.getId(), adminRole.getId());
        adminToken = login("review.admin@bazaarhub.com", "Password@123");

        // 6. Seed Catalog (Category, Brand, Product, Variant)
        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Audio")
                .slug("audio-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        BrandEntity brand = brandRepository.save(BrandEntity.builder()
                .name("SoundPro")
                .slug("soundpro-" + UUID.randomUUID().toString().substring(0, 8))
                .build());

        product = productRepository.save(ProductEntity.builder()
                .seller(seller)
                .category(category)
                .brand(brand)
                .name("SoundPro Elite Wireless Headphones")
                .slug("soundpro-elite-" + UUID.randomUUID().toString().substring(0, 8))
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        variant = variantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .sku("SP-ELITE-BLK-" + UUID.randomUUID().toString().substring(0, 6))
                .variantName("Black / Standard")
                .sellingPrice(new BigDecimal("4999.00"))
                .mrp(new BigDecimal("5999.00"))
                .build());

        // 7. Seed Address & Delivered Order for Customer 1
        AddressEntity address = addressRepository.save(AddressEntity.builder()
                .user(customerUser)
                .recipientName("Customer Ajay")
                .phone("+919876541001")
                .addressLine1("123 MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .build());

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .user(customerUser)
                .shippingAddress(address)
                .recipientName("Customer Ajay")
                .recipientPhone("+919876541001")
                .addressLine1("123 MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .subtotal(new BigDecimal("4999.00"))
                .deliveryFee(BigDecimal.ZERO)
                .grandTotal(new BigDecimal("4999.00"))
                .paymentStatus(PaymentStatus.PAID)
                .status(OrderStatus.DELIVERED)
                .build());

        SellerOrderEntity sellerOrder = sellerOrderRepository.save(SellerOrderEntity.builder()
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(SellerOrderStatus.DELIVERED)
                .subtotal(new BigDecimal("4999.00"))
                .totalAmount(new BigDecimal("4999.00"))
                .build());

        orderItemRepository.save(OrderItemEntity.builder()
                .sellerOrder(sellerOrder)
                .product(product)
                .variant(variant)
                .productName("SoundPro Elite Wireless Headphones")
                .variantSku(variant.getSku())
                .quantity(1)
                .unitPrice(new BigDecimal("4999.00"))
                .totalPrice(new BigDecimal("4999.00"))
                .build());
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("DELETE FROM review_images");
        jdbcTemplate.execute("DELETE FROM product_reviews");
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
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM addresses");
        jdbcTemplate.execute("DELETE FROM stores");
        jdbcTemplate.execute("DELETE FROM sellers");
        jdbcTemplate.execute("DELETE FROM user_devices");
        jdbcTemplate.execute("DELETE FROM user_roles");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
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

    @Test
    @DisplayName("Verified customer successfully submits review and auto-approves")
    void testSubmitReview_verifiedPurchase_success() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Exceptional sound quality")
                .reviewText("Deep bass, crystal clear treble and active noise cancelling is top notch.")
                .imageUrls(List.of("https://example.com/photos/headphones1.jpg"))
                .build();

        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.title").value("Exceptional sound quality"))
                .andExpect(jsonPath("$.data.verifiedPurchase").value(true))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.images", hasSize(1)));
    }

    @Test
    @DisplayName("Ineligible customer without delivered order receives 400 Bad Request")
    void testSubmitReview_ineligibleCustomer_returns400() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(4)
                .title("Looks great")
                .reviewText("Reviewing without buying the product.")
                .build();

        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + secondCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("ineligible to review")));
    }

    @Test
    @DisplayName("Customer attempting duplicate review for same product receives 409 Conflict")
    void testSubmitReview_duplicateReview_returns409() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("First review")
                .reviewText("Great initial purchase.")
                .build();

        // 1. Submit first review
        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Submit second review for same product
        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already submitted a review")));
    }

    @Test
    @DisplayName("Public product reviews endpoint returns masked reviewer names and excludes private fields")
    void testPublicGetReviews_privacyMasked() throws Exception {
        // 1. Customer submits review
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Public test review")
                .reviewText("Verifying public privacy protection.")
                .build();

        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Public anonymous access
        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].reviewerName").value("Customer A."))
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].phone").doesNotExist());
    }

    @Test
    @DisplayName("Public rating summary calculates average and distribution statistics")
    void testPublicRatingSummary() throws Exception {
        // Submit 5-star review
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Five star review")
                .reviewText("Solid five stars.")
                .build();

        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/{productId}/rating-summary", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(product.getId()))
                .andExpect(jsonPath("$.data.totalReviews").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(5.0))
                .andExpect(jsonPath("$.data.fiveStarCount").value(1))
                .andExpect(jsonPath("$.data.fiveStarPercentage").value(100.0));
    }

    @Test
    @DisplayName("Customer can update their own review and retains approved status")
    void testCustomerUpdateReview_success() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Initial rating")
                .reviewText("First impression.")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reviewId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Update review
        UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                .rating(4)
                .title("Updated after usage")
                .reviewText("Updated to 4 stars after battery test.")
                .build();

        mockMvc.perform(put("/api/v1/customer/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.title").value("Updated after usage"))
                .andExpect(jsonPath("$.data.status").value("approved"));
    }

    @Test
    @DisplayName("Customer can delete their own review")
    void testCustomerDeleteReview_success() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Delete test")
                .reviewText("Will be deleted.")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reviewId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/customer/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review deleted successfully"));

        // Verify review is deleted
        mockMvc.perform(get("/api/v1/customer/reviews/{id}", reviewId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Seller can view reviews for their own products")
    void testSellerGetReviews_success() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(5)
                .title("Store review test")
                .reviewText("Seller should be able to view this review.")
                .build();

        mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/seller/reviews")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productId").value(product.getId()));
    }

    @Test
    @DisplayName("Admin can moderate review to rejected and hide it from public")
    void testAdminModerateReview_rejectsAndHidesFromPublic() throws Exception {
        CreateReviewRequest request = CreateReviewRequest.builder()
                .productId(product.getId())
                .rating(1)
                .title("Spam link test")
                .reviewText("Visit http://spam-site.com for discounts.")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/customer/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long reviewId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 1. Admin rejects the review
        ModerateReviewRequest moderate = ModerateReviewRequest.builder()
                .status(ReviewStatus.rejected)
                .reason("Spam and external promotional links")
                .build();

        mockMvc.perform(patch("/api/v1/admin/reviews/{id}/moderate", reviewId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moderate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        // 2. Public endpoint should now return 0 approved reviews
        mockMvc.perform(get("/api/v1/products/{productId}/reviews", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
