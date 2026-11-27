package com.bazaarhub.backend.order.infrastructure.persistence.repository;

import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface SpringDataOrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findBySellerOrderId(Long sellerOrderId);

    List<OrderItemEntity> findBySellerOrderIdIn(Collection<Long> sellerOrderIds);

    @Query("""
        SELECT COUNT(oi) > 0 
        FROM OrderItemEntity oi 
        WHERE oi.product.id = :productId 
          AND oi.sellerOrder.order.user.id = :customerId 
          AND oi.sellerOrder.status = com.bazaarhub.backend.order.domain.model.SellerOrderStatus.DELIVERED
    """)
    boolean hasCustomerPurchasedAndReceivedProduct(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );

    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.totalPrice) FROM OrderItemEntity oi WHERE oi.createdAt BETWEEN :start AND :end GROUP BY oi.product.id, oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProductsByVolume(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.totalPrice) FROM OrderItemEntity oi WHERE oi.createdAt BETWEEN :start AND :end GROUP BY oi.product.id, oi.productName ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> findTopSellingProductsByRevenue(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.totalPrice) FROM OrderItemEntity oi WHERE oi.sellerOrder.seller.id = :sellerId AND oi.createdAt BETWEEN :start AND :end GROUP BY oi.product.id, oi.productName ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> findSellerTopSellingProducts(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end, Pageable pageable);

    @Query("SELECT oi.product.category.id, oi.product.category.name, COUNT(DISTINCT oi.sellerOrder.id), SUM(oi.quantity), SUM(oi.totalPrice) FROM OrderItemEntity oi WHERE oi.createdAt BETWEEN :start AND :end GROUP BY oi.product.category.id, oi.product.category.name ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> aggregateCategorySalesPerformance(@Param("start") Instant start, @Param("end") Instant end);
}

