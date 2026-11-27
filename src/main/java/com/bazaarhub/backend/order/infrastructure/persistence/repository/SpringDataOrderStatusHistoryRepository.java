package com.bazaarhub.backend.order.infrastructure.persistence.repository;

import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataOrderStatusHistoryRepository extends JpaRepository<OrderStatusHistoryEntity, Long> {

    List<OrderStatusHistoryEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<OrderStatusHistoryEntity> findBySellerOrderIdOrderByCreatedAtDesc(Long sellerOrderId);

    List<OrderStatusHistoryEntity> findByOrderIdOrSellerOrderIdOrderByCreatedAtDesc(Long orderId, Long sellerOrderId);
}
