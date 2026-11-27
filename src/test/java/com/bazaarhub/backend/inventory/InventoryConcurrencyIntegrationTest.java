package com.bazaarhub.backend.inventory;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataCategoryRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductRepository;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataStockReservationRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyIntegrationTest {

    @Autowired
    private StockReservationService reservationService;

    @Autowired
    private SpringDataInventoryRepository inventoryRepository;

    @Autowired
    private SpringDataStockReservationRepository reservationRepository;

    @Autowired
    private SpringDataProductVariantRepository variantRepository;

    @Autowired
    private SpringDataProductRepository productRepository;

    @Autowired
    private SpringDataCategoryRepository categoryRepository;

    @Autowired
    private SpringDataSellerRepository sellerRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long variantId;

    @BeforeEach
    void setUp() {
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
        userRepository.deleteAll();

        UserEntity user = userRepository.save(UserEntity.builder()
                .fullName("Seller Concurrency")
                .email("concurrency@seller.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        SellerEntity seller = sellerRepository.save(SellerEntity.builder()
                .user(user)
                .businessName("Concurrency Corp")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build());

        CategoryEntity category = categoryRepository.save(CategoryEntity.builder()
                .name("Flash Sale")
                .slug("flash-sale")
                .status(CatalogStatus.ACTIVE)
                .build());

        ProductEntity product = productRepository.save(ProductEntity.builder()
                .seller(seller)
                .category(category)
                .name("Limited Edition GPU")
                .slug("limited-gpu")
                .status(ProductStatus.ACTIVE)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        ProductVariantEntity variant = variantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .sku("GPU-RTX-4090-LTD")
                .variantName("RTX 4090 Limited Edition")
                .sellingPrice(new BigDecimal("1999.00"))
                .mrp(new BigDecimal("1999.00"))
                .status(CatalogStatus.ACTIVE)
                .build());
        variantId = variant.getId();

        // 5 units total available in stock
        inventoryRepository.save(InventoryEntity.builder()
                .variant(variant)
                .totalQuantity(5)
                .reservedQuantity(0)
                .safetyStock(1)
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Concurrent Reservations: 20 simultaneous threads competing for 5 units guarantees zero overselling")
    void testConcurrentReservations_zeroOverselling() throws Exception {
        int threadCount = 20;
        int totalUnitsAvailable = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 1; i <= threadCount; i++) {
            final String checkoutRef = "CHK-CONCUR-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // simultaneous release of all threads
                    StockReserveRequest request = StockReserveRequest.builder()
                            .variantId(variantId)
                            .checkoutReference(checkoutRef)
                            .quantity(1)
                            .ttlMinutes(15)
                            .build();

                    reservationService.reserveStock(request, null);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    rejectedCount.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected error
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();

        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, finished, "All threads should complete within 30 seconds");
        assertEquals(totalUnitsAvailable, successCount.get(), "Exactly 5 threads should successfully acquire reservations");
        assertEquals(threadCount - totalUnitsAvailable, rejectedCount.get(), "Remaining 15 threads must receive InsufficientStockException");

        // Verify final inventory state in database
        InventoryEntity finalInventory = inventoryRepository.findByVariantId(variantId).orElseThrow();
        assertEquals(5, finalInventory.getTotalQuantity());
        assertEquals(5, finalInventory.getReservedQuantity());
        assertEquals(0, finalInventory.getAvailableQuantity());

        // Verify active reservations count
        long activeReservations = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .count();
        assertEquals(5, activeReservations);
    }
}
