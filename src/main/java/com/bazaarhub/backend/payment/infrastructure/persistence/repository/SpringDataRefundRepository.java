package com.bazaarhub.backend.payment.infrastructure.persistence.repository;

import com.bazaarhub.backend.payment.infrastructure.persistence.entity.RefundEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRefundRepository extends JpaRepository<RefundEntity, Long>, JpaSpecificationExecutor<RefundEntity> {

    Optional<RefundEntity> findByRefundReference(String refundReference);

    Optional<RefundEntity> findByGatewayRefundId(String gatewayRefundId);

    List<RefundEntity> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    List<RefundEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("SELECT r FROM RefundEntity r WHERE r.order.user.id = :userId ORDER BY r.createdAt DESC")
    Page<RefundEntity> findByOrderUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RefundEntity r WHERE r.payment.id = :paymentId AND r.status IN ('COMPLETED', 'PROCESSING', 'APPROVED')")
    BigDecimal getTotalCommittedRefundAmount(@Param("paymentId") Long paymentId);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RefundEntity r WHERE r.status = com.bazaarhub.backend.payment.domain.model.RefundStatus.COMPLETED AND r.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedRefundsBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    @Query("SELECT COUNT(r) FROM RefundEntity r WHERE r.createdAt BETWEEN :start AND :end")
    long countRefundsBetween(@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);

    boolean existsByRefundReference(String refundReference);

    boolean existsByGatewayRefundId(String gatewayRefundId);
}
