package com.bazaarhub.backend.payment.infrastructure.persistence.repository;

import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, Long>, JpaSpecificationExecutor<PaymentEntity> {

    Optional<PaymentEntity> findByPaymentReference(String paymentReference);

    Optional<PaymentEntity> findByGatewayOrderId(String gatewayOrderId);

    Optional<PaymentEntity> findByGatewayPaymentId(String gatewayPaymentId);

    Optional<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findAllByOrderId(Long orderId);

    Optional<PaymentEntity> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentEntity> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    @Query("SELECT p FROM PaymentEntity p WHERE p.order.user.id = :userId")
    Page<PaymentEntity> findByOrderUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id AND p.order.user.id = :userId")
    Optional<PaymentEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.order.id = :orderId AND p.order.user.id = :userId ORDER BY p.createdAt DESC")
    List<PaymentEntity> findByOrderIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

    boolean existsByPaymentReference(String paymentReference);

    boolean existsByGatewayOrderId(String gatewayOrderId);
}
