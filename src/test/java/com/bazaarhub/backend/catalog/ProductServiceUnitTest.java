package com.bazaarhub.backend.catalog;

import com.bazaarhub.backend.catalog.application.dto.request.*;
import com.bazaarhub.backend.catalog.application.dto.response.*;
import com.bazaarhub.backend.catalog.application.service.AdminProductService;
import com.bazaarhub.backend.catalog.application.service.ProductImageService;
import com.bazaarhub.backend.catalog.application.service.ProductService;
import com.bazaarhub.backend.catalog.application.service.ProductVariantService;
import com.bazaarhub.backend.catalog.domain.exception.*;
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
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {

    @Mock
    private SpringDataProductRepository productRepository;
    @Mock
    private SpringDataCategoryRepository categoryRepository;
    @Mock
    private SpringDataBrandRepository brandRepository;
    @Mock
    private SpringDataSellerRepository sellerRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private SpringDataProductImageRepository imageRepository;
    @Mock
    private SpringDataProductVariantRepository variantRepository;

    private ProductService productService;
    private ProductImageService productImageService;
    private ProductVariantService productVariantService;
    private AdminProductService adminProductService;

    private ObjectMapper objectMapper;

    private UserEntity sellerUser;
    private UserEntity adminUser;
    private SellerEntity approvedSeller;
    private CategoryEntity activeCategory;
    private BrandEntity activeBrand;
    private ProductEntity sampleProduct;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        productService = new ProductService(productRepository, categoryRepository, brandRepository, sellerRepository, userRepository);
        productImageService = new ProductImageService(imageRepository, productService, userRepository);
        productVariantService = new ProductVariantService(variantRepository, productService, userRepository, objectMapper);
        adminProductService = new AdminProductService(productRepository, userRepository);

        sellerUser = UserEntity.builder().id(10L).fullName("Test Seller").email("seller@test.com").build();
        adminUser = UserEntity.builder().id(1L).fullName("Admin User").email("admin@test.com").build();

        approvedSeller = SellerEntity.builder()
                .id(100L)
                .user(sellerUser)
                .businessName("Nexus Retail")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        activeCategory = CategoryEntity.builder()
                .id(5L)
                .name("Smartphones")
                .slug("smartphones")
                .status(CatalogStatus.ACTIVE)
                .build();

        activeBrand = BrandEntity.builder()
                .id(8L)
                .name("Apex Brand")
                .slug("apex-brand")
                .status(CatalogStatus.ACTIVE)
                .build();

        sampleProduct = ProductEntity.builder()
                .id(500L)
                .seller(approvedSeller)
                .category(activeCategory)
                .brand(activeBrand)
                .name("Nexus Pro 12")
                .slug("nexus-pro-12")
                .status(ProductStatus.DRAFT)
                .approvalStatus(ProductApprovalStatus.PENDING)
                .createdBy(sellerUser)
                .updatedBy(sellerUser)
                .images(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();
    }

    // =========================================================================
    // ProductService Tests
    // =========================================================================

    @Nested
    @DisplayName("Product Lifecycle & CRUD Tests")
    class ProductCrudTests {

        @Test
        @DisplayName("Create product successfully with valid details")
        void createProduct_success() {
            ProductCreateRequest request = ProductCreateRequest.builder()
                    .name("Nexus Pro 12")
                    .categoryId(5L)
                    .brandId(8L)
                    .description("Flagship smartphone")
                    .status(ProductStatus.DRAFT)
                    .build();

            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(activeCategory));
            when(brandRepository.findById(8L)).thenReturn(Optional.of(activeBrand));
            when(productRepository.existsBySlug("nexus-pro-12")).thenReturn(false);
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
                ProductEntity entity = invocation.getArgument(0);
                entity.setId(500L);
                return entity;
            });

            ProductDetailResponse response = productService.createProduct(10L, request);

            assertNotNull(response);
            assertEquals(500L, response.getId());
            assertEquals("nexus-pro-12", response.getSlug());
            assertEquals(ProductStatus.DRAFT, response.getStatus());
            assertEquals(ProductApprovalStatus.PENDING, response.getApprovalStatus());
            verify(productRepository).save(any(ProductEntity.class));
        }

        @Test
        @DisplayName("Create product fails when seller is unapproved")
        void createProduct_failsWhenSellerNotApproved() {
            SellerEntity pendingSeller = SellerEntity.builder()
                    .id(101L)
                    .user(sellerUser)
                    .approvalStatus(SellerApprovalStatus.PENDING)
                    .build();

            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(pendingSeller));

            ProductCreateRequest request = ProductCreateRequest.builder()
                    .name("Nexus Pro 12")
                    .categoryId(5L)
                    .build();

            assertThrows(UnauthorizedProductAccessException.class, () -> productService.createProduct(10L, request));
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create product fails when category is inactive")
        void createProduct_failsWhenCategoryInactive() {
            CategoryEntity inactiveCat = CategoryEntity.builder()
                    .id(5L)
                    .status(CatalogStatus.INACTIVE)
                    .build();

            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(inactiveCat));

            ProductCreateRequest request = ProductCreateRequest.builder()
                    .name("Nexus Pro 12")
                    .categoryId(5L)
                    .build();

            assertThrows(InvalidProductStateException.class, () -> productService.createProduct(10L, request));
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create product fails when slug already in use")
        void createProduct_failsWhenSlugExists() {
            when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(approvedSeller));
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(activeCategory));
            when(productRepository.existsBySlug("nexus-pro-12")).thenReturn(true);

            ProductCreateRequest request = ProductCreateRequest.builder()
                    .name("Nexus Pro 12")
                    .slug("nexus-pro-12")
                    .categoryId(5L)
                    .build();

            assertThrows(ProductAlreadyExistsException.class, () -> productService.createProduct(10L, request));
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update product fails when not owner")
        void updateProduct_failsWhenNotOwner() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));

            ProductUpdateRequest request = ProductUpdateRequest.builder()
                    .name("Changed Name")
                    .categoryId(5L)
                    .build();

            // userId 999 is not sellerUser (id 10)
            assertThrows(UnauthorizedProductAccessException.class,
                    () -> productService.updateProduct(999L, 500L, request));
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cannot activate a rejected product without approval")
        void updateStatus_failsWhenActivatingRejectedProduct() {
            sampleProduct.setApprovalStatus(ProductApprovalStatus.REJECTED);
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));

            assertThrows(InvalidProductStateException.class,
                    () -> productService.updateProductStatus(10L, 500L, ProductStatus.ACTIVE));
        }

        @Test
        @DisplayName("Delete product soft-deletes to ARCHIVED status")
        void deleteProduct_archivesProduct() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);

            productService.deleteProduct(10L, 500L);

            assertEquals(ProductStatus.ARCHIVED, sampleProduct.getStatus());
            verify(productRepository).save(sampleProduct);
        }
    }

    // =========================================================================
    // ProductImageService Tests
    // =========================================================================

    @Nested
    @DisplayName("Product Image Management Tests")
    class ProductImageTests {

        @Test
        @DisplayName("Add first image automatically designates it as primary")
        void addImage_firstImageIsPrimary() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(imageRepository.existsByProductId(500L)).thenReturn(false);
            when(imageRepository.countByProductId(500L)).thenReturn(0L);
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);
            when(imageRepository.save(any(ProductImageEntity.class))).thenAnswer(invocation -> {
                ProductImageEntity img = invocation.getArgument(0);
                img.setId(1001L);
                return img;
            });

            ProductImageRequest request = ProductImageRequest.builder()
                    .imageUrl("https://cdn.bazaarhub.com/sample.jpg")
                    .build();

            ProductImageResponse response = productImageService.addImage(10L, 500L, request);

            assertNotNull(response);
            assertTrue(response.isPrimary());
            verify(imageRepository, never()).resetPrimaryFlags(any());
            verify(imageRepository).save(any(ProductImageEntity.class));
        }

        @Test
        @DisplayName("Add image with isPrimary=true resets other primary flags")
        void addImage_resetsOtherPrimariesWhenRequested() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(imageRepository.existsByProductId(500L)).thenReturn(true);
            when(imageRepository.countByProductId(500L)).thenReturn(1L);
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);
            when(imageRepository.save(any(ProductImageEntity.class))).thenAnswer(invocation -> {
                ProductImageEntity img = invocation.getArgument(0);
                img.setId(1002L);
                return img;
            });

            ProductImageRequest request = ProductImageRequest.builder()
                    .imageUrl("https://cdn.bazaarhub.com/new-primary.jpg")
                    .isPrimary(true)
                    .build();

            ProductImageResponse response = productImageService.addImage(10L, 500L, request);

            assertNotNull(response);
            assertTrue(response.isPrimary());
            verify(imageRepository).resetPrimaryFlags(500L);
        }

        @Test
        @DisplayName("Deleting primary image promotes next image to primary")
        void deleteImage_promotesNextImageWhenPrimaryDeleted() {
            ProductImageEntity primaryImg = ProductImageEntity.builder()
                    .id(1001L)
                    .product(sampleProduct)
                    .isPrimary(true)
                    .build();

            ProductImageEntity secondImg = ProductImageEntity.builder()
                    .id(1002L)
                    .product(sampleProduct)
                    .isPrimary(false)
                    .build();

            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(imageRepository.findByIdAndProductId(1001L, 500L)).thenReturn(Optional.of(primaryImg));
            when(imageRepository.findFirstByProductIdOrderByDisplayOrderAscIdAsc(500L)).thenReturn(Optional.of(secondImg));
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);

            productImageService.deleteImage(10L, 500L, 1001L);

            verify(imageRepository).delete(primaryImg);
            assertTrue(secondImg.isPrimary());
            verify(imageRepository).save(secondImg);
        }
    }

    // =========================================================================
    // ProductVariantService Tests
    // =========================================================================

    @Nested
    @DisplayName("Product Variant Management Tests")
    class ProductVariantTests {

        @Test
        @DisplayName("Create variant successfully with valid pricing and attributes")
        void createVariant_success() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("NEXUS-12-256")).thenReturn(false);
            when(userRepository.getReferenceById(10L)).thenReturn(sellerUser);
            when(variantRepository.save(any(ProductVariantEntity.class))).thenAnswer(invocation -> {
                ProductVariantEntity v = invocation.getArgument(0);
                v.setId(2001L);
                return v;
            });

            ProductVariantCreateRequest request = ProductVariantCreateRequest.builder()
                    .sku("nexus-12-256")
                    .variantName("Nexus Pro 12 - 256GB")
                    .attributes("{\"storage\": \"256GB\"}")
                    .sellingPrice(new BigDecimal("999.00"))
                    .mrp(new BigDecimal("1199.00"))
                    .taxRate(new BigDecimal("18.00"))
                    .build();

            ProductVariantResponse response = productVariantService.createVariant(10L, 500L, request);

            assertNotNull(response);
            assertEquals("NEXUS-12-256", response.getSku());
            assertEquals(new BigDecimal("999.00"), response.getSellingPrice());
            assertEquals(new BigDecimal("1199.00"), response.getMrp());
            verify(variantRepository).save(any(ProductVariantEntity.class));
        }

        @Test
        @DisplayName("Create variant fails when MRP is less than selling price")
        void createVariant_failsWhenMrpLessThanSellingPrice() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("NEXUS-12-256")).thenReturn(false);

            ProductVariantCreateRequest request = ProductVariantCreateRequest.builder()
                    .sku("nexus-12-256")
                    .variantName("Nexus Pro 12 - 256GB")
                    .sellingPrice(new BigDecimal("1200.00"))
                    .mrp(new BigDecimal("1000.00")) // Invalid: MRP < sellingPrice
                    .build();

            assertThrows(InvalidProductStateException.class,
                    () -> productVariantService.createVariant(10L, 500L, request));
            verify(variantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create variant fails when SKU is duplicate")
        void createVariant_failsWhenSkuDuplicate() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

            ProductVariantCreateRequest request = ProductVariantCreateRequest.builder()
                    .sku("duplicate-sku")
                    .variantName("Nexus Pro 12 - 256GB")
                    .sellingPrice(new BigDecimal("999.00"))
                    .mrp(new BigDecimal("1199.00"))
                    .build();

            assertThrows(SkuAlreadyExistsException.class,
                    () -> productVariantService.createVariant(10L, 500L, request));
            verify(variantRepository, never()).save(any());
        }

        @Test
        @DisplayName("Create variant fails when attributes string is invalid JSON")
        void createVariant_failsWhenAttributesNotValidJson() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.existsBySku("NEXUS-12-256")).thenReturn(false);

            ProductVariantCreateRequest request = ProductVariantCreateRequest.builder()
                    .sku("nexus-12-256")
                    .variantName("Nexus Pro 12 - 256GB")
                    .attributes("INVALID_NOT_JSON")
                    .sellingPrice(new BigDecimal("999.00"))
                    .mrp(new BigDecimal("1199.00"))
                    .build();

            assertThrows(InvalidProductStateException.class,
                    () -> productVariantService.createVariant(10L, 500L, request));
            verify(variantRepository, never()).save(any());
        }
    }

    // =========================================================================
    // AdminProductService Tests
    // =========================================================================

    @Nested
    @DisplayName("Admin Product Moderation Tests")
    class AdminProductTests {

        @Test
        @DisplayName("Approve product sets status to APPROVED and ACTIVE")
        void approveProduct_success() {
            sampleProduct.setStatus(ProductStatus.DRAFT);
            sampleProduct.setApprovalStatus(ProductApprovalStatus.PENDING);

            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(userRepository.getReferenceById(1L)).thenReturn(adminUser);
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

            ProductDetailResponse response = adminProductService.approveProduct(1L, 500L);

            assertNotNull(response);
            assertEquals(ProductApprovalStatus.APPROVED, response.getApprovalStatus());
            assertEquals(ProductStatus.ACTIVE, response.getStatus());
            assertNull(response.getRejectionReason());
            verify(productRepository).save(sampleProduct);
        }

        @Test
        @DisplayName("Reject product sets status to REJECTED and INACTIVE with reason")
        void rejectProduct_success() {
            when(productRepository.findById(500L)).thenReturn(Optional.of(sampleProduct));
            when(userRepository.getReferenceById(1L)).thenReturn(adminUser);
            when(productRepository.save(any(ProductEntity.class))).thenAnswer(i -> i.getArgument(0));

            ProductDetailResponse response = adminProductService.rejectProduct(1L, 500L, "Low quality image and insufficient specs");

            assertNotNull(response);
            assertEquals(ProductApprovalStatus.REJECTED, response.getApprovalStatus());
            assertEquals(ProductStatus.INACTIVE, response.getStatus());
            assertEquals("Low quality image and insufficient specs", response.getRejectionReason());
            verify(productRepository).save(sampleProduct);
        }

        @Test
        @DisplayName("Reject product fails if rejection reason is blank")
        void rejectProduct_failsWhenReasonBlank() {
            assertThrows(InvalidProductStateException.class,
                    () -> adminProductService.rejectProduct(1L, 500L, "   "));
            verify(productRepository, never()).save(any());
        }
    }
}
