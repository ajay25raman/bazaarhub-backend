package com.bazaarhub.backend.search;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.BrandEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataBrandRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductImageRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataBrandRepository brandRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataProductVariantRepository variantRepository;

    @Autowired
    private SpringDataProductImageRepository imageRepository;

    @Autowired
    private SpringDataInventoryRepository inventoryRepository;

    private CategoryEntity electronicsCat;
    private CategoryEntity laptopsCat;
    private CategoryEntity phonesCat;
    private BrandEntity appleBrand;
    private BrandEntity dellBrand;
    private SellerEntity approvedSeller;
    private SellerEntity suspendedSeller;
    private ProductEntity macbookPro;
    private ProductEntity iphone15;
    private ProductEntity dellXps;

    @BeforeEach
    void setUp() {
        // Clean dependent tables to guarantee test data isolation
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
        imageRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        brandRepository.deleteAll();
        sellerRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Sellers
        UserEntity sellerUser1 = userRepository.save(UserEntity.builder()
                .fullName("Apex Merchant")
                .email("apex@merchant.com")
                .passwordHash("$2a$12$DummyHashForIntegrationTestingPurposesOnly1234567890")
                .phone("+919999000001")
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        approvedSeller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser1)
                .businessName("Apex Retailers")
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        UserEntity sellerUser2 = userRepository.save(UserEntity.builder()
                .fullName("Suspended Merchant")
                .email("suspended@merchant.com")
                .passwordHash("$2a$12$DummyHashForIntegrationTestingPurposesOnly1234567890")
                .phone("+919999000002")
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        suspendedSeller = sellerRepository.save(SellerEntity.builder()
                .user(sellerUser2)
                .businessName("Suspended Goods")
                .verificationStatus(SellerVerificationStatus.VERIFIED)
                .approvalStatus(SellerApprovalStatus.SUSPENDED)
                .build());

        // 2. Create Category Tree (Parent: Electronics -> Children: Laptops, Smartphones)
        electronicsCat = categoryRepository.save(CategoryEntity.builder()
                .name("Electronics")
                .slug("electronics")
                .status(CatalogStatus.ACTIVE)
                .displayOrder(1)
                .build());

        laptopsCat = categoryRepository.save(CategoryEntity.builder()
                .parent(electronicsCat)
                .name("Laptops")
                .slug("laptops")
                .status(CatalogStatus.ACTIVE)
                .displayOrder(1)
                .build());

        phonesCat = categoryRepository.save(CategoryEntity.builder()
                .parent(electronicsCat)
                .name("Smartphones")
                .slug("smartphones")
                .status(CatalogStatus.ACTIVE)
                .displayOrder(2)
                .build());

        electronicsCat.setChildren(List.of(laptopsCat, phonesCat));

        // 3. Create Brands
        appleBrand = brandRepository.save(BrandEntity.builder()
                .name("Apple")
                .slug("apple")
                .status(CatalogStatus.ACTIVE)
                .build());

        dellBrand = brandRepository.save(BrandEntity.builder()
                .name("Dell")
                .slug("dell")
                .status(CatalogStatus.ACTIVE)
                .build());

        // 4. Create Active Products
        // Product 1: MacBook Pro (Laptops, Apple, Apex Retailer, Price 249,900, In Stock)
        macbookPro = createProduct("Apple MacBook Pro 16", "apple-macbook-pro-16",
                "High-performance M3 Max laptop for creative professionals",
                laptopsCat, appleBrand, approvedSeller, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED,
                Instant.now().minusSeconds(3600));

        ProductVariantEntity mbpVariant = createVariant(macbookPro, "MBP16-M3-1TB", "16-inch 1TB",
                new BigDecimal("249900.00"), new BigDecimal("269900.00"), CatalogStatus.ACTIVE);
        macbookPro.setVariants(List.of(mbpVariant));
        createImage(macbookPro, "https://cdn.bazaarhub.com/mbp16.webp", true);
        createInventory(mbpVariant, 20, 2); // available = 18 (in stock)

        // Product 2: iPhone 15 Pro (Smartphones, Apple, Apex Retailer, Price 129,900, Out of Stock)
        iphone15 = createProduct("Apple iPhone 15 Pro", "apple-iphone-15-pro",
                "Titanium design A17 Pro flagship smartphone with advanced camera",
                phonesCat, appleBrand, approvedSeller, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED,
                Instant.now().minusSeconds(1800));

        ProductVariantEntity iphoneVariant = createVariant(iphone15, "IPHONE15-PRO-256", "256GB Black",
                new BigDecimal("129900.00"), new BigDecimal("139900.00"), CatalogStatus.ACTIVE);
        iphone15.setVariants(List.of(iphoneVariant));
        createImage(iphone15, "https://cdn.bazaarhub.com/iphone15.webp", true);
        createInventory(iphoneVariant, 0, 0); // available = 0 (out of stock)

        // Product 3: Dell XPS 15 (Laptops, Dell, Apex Retailer, Price 189,900, In Stock)
        dellXps = createProduct("Dell XPS 15", "dell-xps-15",
                "InfinityEdge display creator laptop with Intel Core i9 processor",
                laptopsCat, dellBrand, approvedSeller, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED,
                Instant.now());

        ProductVariantEntity dellVariant = createVariant(dellXps, "DELL-XPS-15-I9", "i9 32GB 1TB",
                new BigDecimal("189900.00"), new BigDecimal("209900.00"), CatalogStatus.ACTIVE);
        dellXps.setVariants(List.of(dellVariant));
        createImage(dellXps, "https://cdn.bazaarhub.com/dellxps.webp", true);
        createInventory(dellVariant, 10, 1); // available = 9 (in stock)

        // Product 4: Draft product (Should be excluded)
        ProductEntity draftProduct = createProduct("Apple MacBook Air Draft", "apple-macbook-air-draft",
                "Draft air model not yet published",
                laptopsCat, appleBrand, approvedSeller, ProductStatus.DRAFT, ProductApprovalStatus.PENDING,
                Instant.now());
        ProductVariantEntity draftVariant = createVariant(draftProduct, "MBAIR-DRAFT", "Draft Variant",
                new BigDecimal("99900.00"), new BigDecimal("109900.00"), CatalogStatus.ACTIVE);
        draftProduct.setVariants(List.of(draftVariant));

        // Product 5: Product from Suspended Seller (Should be excluded)
        ProductEntity suspendedSellerProd = createProduct("Dell Inspiron Cheap", "dell-inspiron-cheap",
                "Budget laptop from suspended seller",
                laptopsCat, dellBrand, suspendedSeller, ProductStatus.ACTIVE, ProductApprovalStatus.APPROVED,
                Instant.now());
        ProductVariantEntity suspVariant = createVariant(suspendedSellerProd, "DELL-INSP-CHEAP", "Cheap Variant",
                new BigDecimal("49900.00"), new BigDecimal("59900.00"), CatalogStatus.ACTIVE);
        suspendedSellerProd.setVariants(List.of(suspVariant));

        // Product 6: Rejected / Inactive Product (Should be excluded)
        ProductEntity rejectedProd = createProduct("Apple Prototype Phone", "apple-prototype-phone",
                "Rejected phone prototype",
                phonesCat, appleBrand, approvedSeller, ProductStatus.INACTIVE, ProductApprovalStatus.REJECTED,
                Instant.now());
        ProductVariantEntity rejVariant = createVariant(rejectedProd, "PROTO-PHONE", "Proto Variant",
                new BigDecimal("300000.00"), new BigDecimal("350000.00"), CatalogStatus.ACTIVE);
        rejectedProd.setVariants(List.of(rejVariant));
    }

    // -------------------------------------------------------------------------
    // Scenario 1: Product search by name
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 1: Product search by exact/partial title matches expected product")
    void test01_SearchByName() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "MacBook Pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Apple MacBook Pro 16")))
                .andExpect(jsonPath("$.data.content[0].slug", is("apple-macbook-pro-16")));
    }

    // -------------------------------------------------------------------------
    // Scenario 2: Partial keyword search
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 2: Partial keyword search matches substring in title or description")
    void test02_PartialKeywordSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "XPS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Dell XPS 15")));
    }

    // -------------------------------------------------------------------------
    // Scenario 3: Case-insensitive search
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 3: Case-insensitive search correctly matches regardless of casing")
    void test03_CaseInsensitiveSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "iPhOnE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Apple iPhone 15 Pro")));
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Empty search input
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 4: Empty or missing search keyword returns all publicly visible products")
    void test04_EmptySearchInput() throws Exception {
        mockMvc.perform(get("/api/v1/search/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.totalElements", is(3)));
    }

    // -------------------------------------------------------------------------
    // Scenario 5: Search with no matching products
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 5: Search with no matching keywords returns empty list with 200 OK")
    void test05_NoMatchingProducts() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "NonExistentGadgetXYZ999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    // -------------------------------------------------------------------------
    // Scenario 6: Category-based discovery
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 6: Category-based discovery returns category and subcategory products")
    void test06_CategoryBasedDiscovery() throws Exception {
        // Parent category (Electronics) includes Laptops and Smartphones
        mockMvc.perform(get("/api/v1/search/categories/{categoryId}/products", electronicsCat.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // Child subcategory (Laptops) includes only MacBook and Dell XPS
        mockMvc.perform(get("/api/v1/search/categories/{categoryId}/products", laptopsCat.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    // -------------------------------------------------------------------------
    // Scenario 7: Minimum and maximum price filters
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 7: Price range filter matches products with variants within price bounds")
    void test07_PriceRangeFilter() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("minPrice", "100000.00")
                        .param("maxPrice", "150000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Apple iPhone 15 Pro")));
    }

    // -------------------------------------------------------------------------
    // Scenario 8: Combined filters
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 8: Combined filters (keyword, category, brand, price, inStock)")
    void test08_CombinedFilters() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "Apple")
                        .param("categoryId", laptopsCat.getId().toString())
                        .param("brandId", appleBrand.getId().toString())
                        .param("minPrice", "200000.00")
                        .param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name", is("Apple MacBook Pro 16")))
                .andExpect(jsonPath("$.data.content[0].inStock", is(true)));
    }

    // -------------------------------------------------------------------------
    // Scenario 9: Invalid price range
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 9: minPrice greater than maxPrice returns 400 Bad Request")
    void test09_InvalidPriceRange() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("minPrice", "300000.00")
                        .param("maxPrice", "100000.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Minimum price cannot be greater than maximum price")));
    }

    // -------------------------------------------------------------------------
    // Scenario 10: Sorting by price
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 10: Sorting by price_asc and price_desc orders by variant prices")
    void test10_SortByPrice() throws Exception {
        // Price ASC: iPhone 15 (129,900) -> Dell XPS (189,900) -> MacBook Pro (249,900)
        mockMvc.perform(get("/api/v1/search/products")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name", is("Apple iPhone 15 Pro")))
                .andExpect(jsonPath("$.data.content[1].name", is("Dell XPS 15")))
                .andExpect(jsonPath("$.data.content[2].name", is("Apple MacBook Pro 16")));

        // Price DESC: MacBook Pro (249,900) -> Dell XPS (189,900) -> iPhone 15 (129,900)
        mockMvc.perform(get("/api/v1/search/products")
                        .param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name", is("Apple MacBook Pro 16")))
                .andExpect(jsonPath("$.data.content[1].name", is("Dell XPS 15")))
                .andExpect(jsonPath("$.data.content[2].name", is("Apple iPhone 15 Pro")));
    }

    // -------------------------------------------------------------------------
    // Scenario 11: Sorting by newest
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 11: Sorting by newest orders products by createdAt descending")
    void test11_SortByNewest() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("sort", "newest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name", is("Dell XPS 15")))
                .andExpect(jsonPath("$.data.content[1].name", is("Apple iPhone 15 Pro")))
                .andExpect(jsonPath("$.data.content[2].name", is("Apple MacBook Pro 16")));
    }

    // -------------------------------------------------------------------------
    // Scenario 12: Pagination
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 12: Pagination controls slice size and total page metrics")
    void test12_Pagination() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements", is(3)))
                .andExpect(jsonPath("$.data.totalPages", is(2)))
                .andExpect(jsonPath("$.data.number", is(0)));

        mockMvc.perform(get("/api/v1/search/products")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.number", is(1)));
    }

    // -------------------------------------------------------------------------
    // Scenario 13: Maximum page-size validation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 13: Page size exceeding 100 returns 400 Bad Request")
    void test13_MaxPageSizeValidation() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("size", "150"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // -------------------------------------------------------------------------
    // Scenario 14: Hidden or inactive product exclusion
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 14: Draft and inactive/rejected products are strictly excluded from search")
    void test14_HiddenAndInactiveProductsExclusion() throws Exception {
        // Querying "Draft" should not return Apple MacBook Air Draft
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "Draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // Querying "Prototype" should not return Apple Prototype Phone
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "Prototype"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Scenario 15: Suspended seller product exclusion
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 15: Products belonging to suspended sellers are strictly excluded from search")
    void test15_SuspendedSellerProductExclusion() throws Exception {
        // "Inspiron" belongs to suspended seller
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "Inspiron"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Scenario 16: Related product discovery
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 16: Related products recommends same category/brand products and excludes target")
    void test16_RelatedProductsDiscovery() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/related/{productId}", macbookPro.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                // Should contain Dell XPS (same category) and iPhone 15 (same brand), but NOT MacBook Pro
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].id", not(hasItem(macbookPro.getId().intValue()))));
    }

    // -------------------------------------------------------------------------
    // Scenario 17: New arrivals discovery
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 17: New arrivals discovery endpoint returns products ordered by createdAt DESC")
    void test17_NewArrivalsDiscovery() throws Exception {
        mockMvc.perform(get("/api/v1/discovery/new-arrivals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[0].name", is("Dell XPS 15")));
    }

    // -------------------------------------------------------------------------
    // Scenario 18: Unauthorized access to protected functionality
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 18: Non-GET HTTP methods and protected administrative routes require authentication")
    void test18_UnauthorizedAccess() throws Exception {
        // Attempt POST to search products (read-only endpoint)
        mockMvc.perform(post("/api/v1/search/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        // Attempt accessing internal admin endpoint without auth
        mockMvc.perform(get("/api/v1/admin/inventory"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // Scenario 19: SQL injection-style search input handling
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 19: Malicious SQL injection payloads in search are safely parameterized")
    void test19_SqlInjectionSafeHandling() throws Exception {
        mockMvc.perform(get("/api/v1/search/products")
                        .param("q", "'; DROP TABLE products; -- ' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // -------------------------------------------------------------------------
    // Scenario 20: Invalid category and product IDs
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 20: Non-existent category or product IDs return 404 Not Found")
    void test20_InvalidCategoryAndProductIds() throws Exception {
        // Non-existent category
        mockMvc.perform(get("/api/v1/search/categories/{categoryId}/products", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Category")));

        // Non-existent product for related discovery
        mockMvc.perform(get("/api/v1/discovery/related/{productId}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Product")));
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    private ProductEntity createProduct(String name, String slug, String description,
                                        CategoryEntity category, BrandEntity brand, SellerEntity seller,
                                        ProductStatus status, ProductApprovalStatus approvalStatus,
                                        Instant createdAt) {
        return productRepository.save(ProductEntity.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .category(category)
                .brand(brand)
                .seller(seller)
                .status(status)
                .approvalStatus(approvalStatus)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }

    private ProductVariantEntity createVariant(ProductEntity product, String sku, String name,
                                               BigDecimal price, BigDecimal mrp, CatalogStatus status) {
        return variantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .sku(sku)
                .variantName(name)
                .sellingPrice(price)
                .mrp(mrp)
                .status(status)
                .taxRate(BigDecimal.ZERO)
                .build());
    }

    private void createImage(ProductEntity product, String url, boolean isPrimary) {
        imageRepository.save(ProductImageEntity.builder()
                .product(product)
                .imageUrl(url)
                .isPrimary(isPrimary)
                .displayOrder(0)
                .build());
    }

    private void createInventory(ProductVariantEntity variant, int total, int reserved) {
        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant)
                .totalQuantity(total)
                .reservedQuantity(reserved)
                .safetyStock(5)
                .version(0L)
                .build());
    }
}
