package com.bazaarhub.backend.wishlist;

import com.bazaarhub.backend.catalog.domain.exception.ProductNotFoundException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.wishlist.application.dto.request.WishlistUpdateRequest;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistItemResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistProductExistsResponse;
import com.bazaarhub.backend.wishlist.application.dto.response.WishlistResponse;
import com.bazaarhub.backend.wishlist.application.service.CustomerWishlistService;
import com.bazaarhub.backend.wishlist.domain.exception.InvalidWishlistOperationException;
import com.bazaarhub.backend.wishlist.domain.exception.WishlistItemAlreadyExistsException;
import com.bazaarhub.backend.wishlist.domain.exception.WishlistItemNotFoundException;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistEntity;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.entity.WishlistItemEntity;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistItemRepository;
import com.bazaarhub.backend.wishlist.infrastructure.persistence.repository.SpringDataWishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerWishlistServiceUnitTest {

    @Mock
    private SpringDataWishlistRepository wishlistRepository;
    @Mock
    private SpringDataWishlistItemRepository wishlistItemRepository;
    @Mock
    private SpringDataProductRepository productRepository;
    @Mock
    private SpringDataUserRepository userRepository;
    @Mock
    private SpringDataInventoryRepository inventoryRepository;

    private CustomerWishlistService wishlistService;

    private UserEntity mockUser;
    private WishlistEntity mockWishlist;
    private ProductEntity activeProduct;
    private ProductEntity draftProduct;
    private ProductVariantEntity mockVariant;
    private InventoryEntity mockInventory;

    @BeforeEach
    void setUp() {
        wishlistService = new CustomerWishlistService(
                wishlistRepository,
                wishlistItemRepository,
                productRepository,
                userRepository,
                inventoryRepository
        );

        mockUser = UserEntity.builder()
                .id(1L)
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .build();

        mockWishlist = WishlistEntity.builder()
                .id(10L)
                .user(mockUser)
                .name("My Wishlist")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CategoryEntity category = CategoryEntity.builder().id(5L).name("Electronics").build();

        activeProduct = ProductEntity.builder()
                .id(100L)
                .name("Smartphone XYZ")
                .slug("smartphone-xyz")
                .category(category)
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build();

        mockVariant = ProductVariantEntity.builder()
                .id(200L)
                .product(activeProduct)
                .sku("PHONE-XYZ-BLK")
                .status(CatalogStatus.ACTIVE)
                .sellingPrice(new BigDecimal("29999.00"))
                .build();
        activeProduct.setVariants(List.of(mockVariant));

        mockInventory = InventoryEntity.builder()
                .id(300L)
                .variant(mockVariant)
                .totalQuantity(50)
                .reservedQuantity(5)
                .safetyStock(5)
                .build();

        draftProduct = ProductEntity.builder()
                .id(101L)
                .name("Draft Item")
                .status(ProductStatus.DRAFT)
                .approvalStatus(ProductApprovalStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Get customer wishlist auto-creates wishlist if not present")
    void testGetCustomerWishlistAutoCreate() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(wishlistRepository.save(any(WishlistEntity.class))).thenReturn(mockWishlist);
        when(wishlistItemRepository.countByWishlistId(10L)).thenReturn(0);

        WishlistResponse response = wishlistService.getCustomerWishlist(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("My Wishlist", response.getName());
        assertEquals(0, response.getTotalItems());
        verify(wishlistRepository).save(any(WishlistEntity.class));
    }

    @Test
    @DisplayName("Rename wishlist updates name successfully")
    void testUpdateWishlistName() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(wishlistRepository.save(any(WishlistEntity.class))).thenReturn(mockWishlist);
        when(wishlistItemRepository.countByWishlistId(10L)).thenReturn(2);

        WishlistUpdateRequest request = WishlistUpdateRequest.builder()
                .name("Gadgets to Buy")
                .build();

        WishlistResponse response = wishlistService.updateWishlistName(1L, request);

        assertNotNull(response);
        assertEquals("Gadgets to Buy", mockWishlist.getName());
        assertEquals(2, response.getTotalItems());
    }

    @Test
    @DisplayName("Add product to wishlist succeeds for active and approved product")
    void testAddProductToWishlistSuccess() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(activeProduct));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(10L, 100L)).thenReturn(false);
        when(inventoryRepository.findByVariantId(200L)).thenReturn(Optional.of(mockInventory));

        when(wishlistItemRepository.save(any(WishlistItemEntity.class))).thenAnswer(i -> {
            WishlistItemEntity item = i.getArgument(0);
            item.setId(500L);
            return item;
        });

        WishlistItemResponse response = wishlistService.addProductToWishlist(1L, 100L);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(100L, response.getProductId());
        assertEquals("Smartphone XYZ", response.getProductName());
        assertTrue(response.isInStock());
        assertEquals(new BigDecimal("29999.00"), response.getBasePrice());
    }

    @Test
    @DisplayName("Add product throws InvalidWishlistOperationException for draft/unapproved product")
    void testAddDraftProductToWishlistThrows() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(101L)).thenReturn(Optional.of(draftProduct));

        assertThrows(InvalidWishlistOperationException.class,
                () -> wishlistService.addProductToWishlist(1L, 101L));
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Add non-existent product throws ProductNotFoundException")
    void testAddNonExistentProductThrows() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> wishlistService.addProductToWishlist(1L, 999L));
    }

    @Test
    @DisplayName("Add duplicate product throws WishlistItemAlreadyExistsException")
    void testAddDuplicateProductThrows() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(activeProduct));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(10L, 100L)).thenReturn(true);

        assertThrows(WishlistItemAlreadyExistsException.class,
                () -> wishlistService.addProductToWishlist(1L, 100L));
    }

    @Test
    @DisplayName("Concurrent duplicate add caught by unique constraint and handled safely")
    void testConcurrentDuplicateAddCaught() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findById(100L)).thenReturn(Optional.of(activeProduct));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(10L, 100L)).thenReturn(false);
        when(wishlistItemRepository.save(any(WishlistItemEntity.class))).thenThrow(new DataIntegrityViolationException("uq_wishlist_items_product"));

        assertThrows(WishlistItemAlreadyExistsException.class,
                () -> wishlistService.addProductToWishlist(1L, 100L));
    }

    @Test
    @DisplayName("Remove product from wishlist succeeds when present")
    void testRemoveProductSuccess() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        WishlistItemEntity item = WishlistItemEntity.builder().id(500L).wishlist(mockWishlist).product(activeProduct).build();
        when(wishlistItemRepository.findByWishlistIdAndProductId(10L, 100L)).thenReturn(Optional.of(item));

        wishlistService.removeProductFromWishlist(1L, 100L);

        verify(wishlistItemRepository).delete(item);
    }

    @Test
    @DisplayName("Remove product throws WishlistItemNotFoundException when not in wishlist")
    void testRemoveProductNotFoundThrows() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(wishlistItemRepository.findByWishlistIdAndProductId(10L, 100L)).thenReturn(Optional.empty());

        assertThrows(WishlistItemNotFoundException.class,
                () -> wishlistService.removeProductFromWishlist(1L, 100L));
    }

    @Test
    @DisplayName("Check product in wishlist returns true when item present")
    void testCheckProductInWishlistTrue() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(10L, 100L)).thenReturn(true);

        WishlistProductExistsResponse res = wishlistService.checkProductInWishlist(1L, 100L);

        assertNotNull(res);
        assertEquals(100L, res.getProductId());
        assertTrue(res.isInWishlist());
    }

    @Test
    @DisplayName("Check product in wishlist returns false when item absent")
    void testCheckProductInWishlistFalse() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));
        when(wishlistItemRepository.existsByWishlistIdAndProductId(10L, 100L)).thenReturn(false);

        WishlistProductExistsResponse res = wishlistService.checkProductInWishlist(1L, 100L);

        assertNotNull(res);
        assertFalse(res.isInWishlist());
    }

    @Test
    @DisplayName("Clear wishlist removes all items for user")
    void testClearWishlist() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));

        wishlistService.clearWishlist(1L);

        verify(wishlistItemRepository).deleteAllByWishlistId(10L);
    }

    @Test
    @DisplayName("Get wishlist items returns paginated items with accurate stock status")
    void testGetWishlistItemsPaged() {
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(mockWishlist));

        WishlistItemEntity item = WishlistItemEntity.builder()
                .id(501L)
                .wishlist(mockWishlist)
                .product(activeProduct)
                .createdAt(Instant.now())
                .build();

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(wishlistItemRepository.findByWishlistIdOrderByCreatedAtDesc(10L, pageRequest))
                .thenReturn(new PageImpl<>(List.of(item), pageRequest, 1));
        when(inventoryRepository.findByVariantId(200L)).thenReturn(Optional.of(mockInventory));

        Page<WishlistItemResponse> page = wishlistService.getWishlistItems(1L, pageRequest);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        WishlistItemResponse dto = page.getContent().get(0);
        assertEquals("Smartphone XYZ", dto.getProductName());
        assertTrue(dto.isInStock());
    }
}
