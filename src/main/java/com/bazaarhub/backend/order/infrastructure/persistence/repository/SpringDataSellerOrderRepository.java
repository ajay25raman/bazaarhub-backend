package com.bazaarhub.backend.order.infrastructure.persistence.repository;

import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataSellerOrderRepository extends JpaRepository<SellerOrderEntity, Long>, JpaSpecificationExecutor<SellerOrderEntity> {

    Optional<SellerOrderEntity> findBySubOrderNumber(String subOrderNumber);

    Optional<SellerOrderEntity> findByIdAndSellerId(Long id, Long sellerId);

    Optional<SellerOrderEntity> findBySubOrderNumberAndSellerId(String subOrderNumber, Long sellerId);

    List<SellerOrderEntity> findByOrderId(Long orderId);

    Optional<SellerOrderEntity> findByOrderIdAndSellerId(Long orderId, Long sellerId);

    Page<SellerOrderEntity> findBySellerId(Long sellerId, Pageable pageable);

    Page<SellerOrderEntity> findBySellerIdAndStatus(Long sellerId, SellerOrderStatus status, Pageable pageable);

    boolean existsBySubOrderNumber(String subOrderNumber);

    @Query("SELECT COUNT(so) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.createdAt BETWEEN :start AND :end")
    long countSellerOrdersBetween(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(so) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.status = :status AND so.createdAt BETWEEN :start AND :end")
    long countSellerOrdersByStatusBetween(@Param("sellerId") Long sellerId, @Param("status") SellerOrderStatus status, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(so.subtotal), 0) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.createdAt BETWEEN :start AND :end")
    BigDecimal sumSubtotalBySellerIdBetween(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(so.commissionAmount), 0) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.createdAt BETWEEN :start AND :end")
    BigDecimal sumCommissionBySellerIdBetween(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(so.totalAmount), 0) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.createdAt BETWEEN :start AND :end")
    BigDecimal sumTotalAmountBySellerIdBetween(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT so.status, COUNT(so) FROM SellerOrderEntity so WHERE so.seller.id = :sellerId AND so.createdAt BETWEEN :start AND :end GROUP BY so.status")
    List<Object[]> aggregateSellerOrderStatusCounts(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT CAST(created_at AS DATE) as order_date, COUNT(id) as total_orders, COALESCE(SUM(subtotal), 0) as total_revenue FROM seller_orders WHERE seller_id = :sellerId AND created_at BETWEEN :start AND :end GROUP BY CAST(created_at AS DATE) ORDER BY order_date ASC", nativeQuery = true)
    List<Object[]> aggregateSellerDailySalesTrends(@Param("sellerId") Long sellerId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT so.seller.id, so.seller.businessName, COUNT(so), COALESCE(SUM(so.subtotal), 0) FROM SellerOrderEntity so WHERE so.createdAt BETWEEN :start AND :end GROUP BY so.seller.id, so.seller.businessName ORDER BY SUM(so.subtotal) DESC")
    List<Object[]> findTopSellersByGMV(@Param("start") Instant start, @Param("end") Instant end, Pageable pageable);
}
