package com.bazaarhub.backend.payment.infrastructure.persistence.repository;

import com.bazaarhub.backend.payment.domain.model.TransactionType;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, Long> {

    List<PaymentTransactionEntity> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    Optional<PaymentTransactionEntity> findByGatewayTransactionId(String gatewayTransactionId);

    boolean existsByGatewayTransactionIdAndTransactionType(String gatewayTransactionId, TransactionType transactionType);
}
