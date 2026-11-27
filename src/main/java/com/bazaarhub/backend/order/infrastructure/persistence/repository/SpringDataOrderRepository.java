package com.bazaarhub.backend.order.infrastructure.persistence.repository;

import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
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
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);

    Optional<OrderEntity> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Page<OrderEntity> findByUserId(Long userId, Pageable pageable);

    Page<OrderEntity> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt BETWEEN :start AND :end")
    long countOrdersBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status AND o.createdAt BETWEEN :start AND :end")
    long countOrdersByStatusBetween(@Param("status") OrderStatus status, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM OrderEntity o WHERE o.createdAt BETWEEN :start AND :end")
    BigDecimal sumGrandTotalBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT o.status, COUNT(o) FROM OrderEntity o WHERE o.createdAt BETWEEN :start AND :end GROUP BY o.status")
    List<Object[]> aggregateOrderStatusCounts(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(DISTINCT o.user.id) FROM OrderEntity o WHERE o.createdAt BETWEEN :start AND :end")
    long countDistinctCustomersBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT COUNT(*) FROM (SELECT customer_id FROM orders WHERE created_at BETWEEN :start AND :end GROUP BY customer_id HAVING COUNT(id) > 1) AS repeat_cust", nativeQuery = true)
    long countRepeatCustomersBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = "SELECT CAST(created_at AS DATE) as order_date, COUNT(id) as total_orders, COALESCE(SUM(total_amount), 0) as total_revenue FROM orders WHERE created_at BETWEEN :start AND :end GROUP BY CAST(created_at AS DATE) ORDER BY order_date ASC", nativeQuery = true)
    List<Object[]> aggregateDailySalesTrends(@Param("start") Instant start, @Param("end") Instant end);
}
