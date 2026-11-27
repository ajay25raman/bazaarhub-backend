package com.bazaarhub.backend.order.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderStatusTransitionException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service managing order and seller sub-order state machine transitions,
 * history audit tracking, and parent/sub-order status synchronization.
 */
@Service
public class OrderStatusTransitionService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusTransitionService.class);

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_ORDER_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.RETURNED),
            OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURNED),
            OrderStatus.CANCELLED, EnumSet.of(OrderStatus.RETURNED),
            OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class)
    );

    private static final Map<SellerOrderStatus, Set<SellerOrderStatus>> VALID_SELLER_ORDER_TRANSITIONS = Map.of(
            SellerOrderStatus.PENDING, EnumSet.of(SellerOrderStatus.CONFIRMED, SellerOrderStatus.CANCELLED),
            SellerOrderStatus.CONFIRMED, EnumSet.of(SellerOrderStatus.PROCESSING, SellerOrderStatus.CANCELLED),
            SellerOrderStatus.PROCESSING, EnumSet.of(SellerOrderStatus.SHIPPED, SellerOrderStatus.CANCELLED),
            SellerOrderStatus.SHIPPED, EnumSet.of(SellerOrderStatus.DELIVERED, SellerOrderStatus.CANCELLED, SellerOrderStatus.RETURNED),
            SellerOrderStatus.DELIVERED, EnumSet.of(SellerOrderStatus.RETURNED),
            SellerOrderStatus.CANCELLED, EnumSet.of(SellerOrderStatus.RETURNED),
            SellerOrderStatus.RETURNED, EnumSet.noneOf(SellerOrderStatus.class)
    );

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;

    public OrderStatusTransitionService(SpringDataOrderRepository orderRepository,
                                        SpringDataSellerOrderRepository sellerOrderRepository,
                                        SpringDataOrderStatusHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.historyRepository = historyRepository;
    }

    /**
     * Validates and transitions the parent order to target status, recording history.
     */
    @Transactional
    public OrderEntity transitionOrderStatus(OrderEntity order, OrderStatus targetStatus, String remarks, UserEntity actor) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == targetStatus) {
            log.info("[ORDER STATUS IDEMPOTENT] Order {} already in status {}", order.getOrderNumber(), targetStatus);
            return order;
        }

        Set<OrderStatus> allowed = VALID_ORDER_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(OrderStatus.class));
        if (!allowed.contains(targetStatus)) {
            throw new InvalidOrderStatusTransitionException(String.format(
                    "Invalid order status transition from %s to %s for order %s",
                    currentStatus, targetStatus, order.getOrderNumber()));
        }

        order.setStatus(targetStatus);
        if (targetStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(Instant.now());
            if (remarks != null && !remarks.isBlank()) {
                order.setCancellationReason(remarks);
            }
        }
        order.setUpdatedBy(actor);
        order.setUpdatedAt(Instant.now());
        OrderEntity savedOrder = orderRepository.save(order);

        recordHistory(savedOrder, null, currentStatus.name(), targetStatus.name(), remarks, actor);

        log.info("[ORDER TRANSITION] Order {} transitioned from {} to {} by user ID: {}",
                order.getOrderNumber(), currentStatus, targetStatus, actor != null ? actor.getId() : "SYSTEM");

        return savedOrder;
    }

    /**
     * Validates and transitions a seller sub-order to target status, recording history
     * and synchronizing parent order status.
     */
    @Transactional
    public SellerOrderEntity transitionSellerOrderStatus(SellerOrderEntity sellerOrder,
                                                         SellerOrderStatus targetStatus,
                                                         String remarks,
                                                         UserEntity actor) {
        SellerOrderStatus currentStatus = sellerOrder.getStatus();

        if (currentStatus == targetStatus) {
            log.info("[SELLER ORDER STATUS IDEMPOTENT] Sub-order {} already in status {}",
                    sellerOrder.getSubOrderNumber(), targetStatus);
            return sellerOrder;
        }

        Set<SellerOrderStatus> allowed = VALID_SELLER_ORDER_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(SellerOrderStatus.class));
        if (!allowed.contains(targetStatus)) {
            throw new InvalidOrderStatusTransitionException(String.format(
                    "Invalid seller sub-order status transition from %s to %s for sub-order %s",
                    currentStatus, targetStatus, sellerOrder.getSubOrderNumber()));
        }

        sellerOrder.setStatus(targetStatus);
        if (targetStatus == SellerOrderStatus.CANCELLED) {
            sellerOrder.setCancelledAt(Instant.now());
            if (remarks != null && !remarks.isBlank()) {
                sellerOrder.setCancellationReason(remarks);
            }
        }
        sellerOrder.setUpdatedBy(actor);
        sellerOrder.setUpdatedAt(Instant.now());
        SellerOrderEntity savedSellerOrder = sellerOrderRepository.save(sellerOrder);

        recordHistory(savedSellerOrder.getOrder(), savedSellerOrder, currentStatus.name(), targetStatus.name(), remarks, actor);

        log.info("[SELLER ORDER TRANSITION] Sub-order {} transitioned from {} to {} by user ID: {}",
                sellerOrder.getSubOrderNumber(), currentStatus, targetStatus, actor != null ? actor.getId() : "SYSTEM");

        // Synchronize parent order status based on all sibling sub-orders
        syncParentOrderStatus(savedSellerOrder.getOrder(), actor);

        return savedSellerOrder;
    }

    /**
     * Synchronizes parent order status with the aggregate states of its seller sub-orders.
     */
    @Transactional
    public void syncParentOrderStatus(OrderEntity order, UserEntity actor) {
        List<SellerOrderEntity> subOrders = sellerOrderRepository.findByOrderId(order.getId());
        if (subOrders.isEmpty()) {
            return;
        }

        boolean allDelivered = subOrders.stream().allMatch(so -> so.getStatus() == SellerOrderStatus.DELIVERED);
        boolean allCancelled = subOrders.stream().allMatch(so -> so.getStatus() == SellerOrderStatus.CANCELLED);
        boolean anyShipped = subOrders.stream().anyMatch(so -> so.getStatus() == SellerOrderStatus.SHIPPED);
        boolean anyProcessing = subOrders.stream().anyMatch(so ->
                so.getStatus() == SellerOrderStatus.PROCESSING);
        boolean allConfirmedOrBeyond = subOrders.stream().allMatch(so ->
                so.getStatus() != SellerOrderStatus.PENDING);

        OrderStatus targetStatus = null;
        String syncRemarks = null;

        if (allDelivered && order.getStatus() != OrderStatus.DELIVERED) {
            targetStatus = OrderStatus.DELIVERED;
            syncRemarks = "All seller sub-orders delivered. Parent order marked DELIVERED.";
        } else if (allCancelled && order.getStatus() != OrderStatus.CANCELLED) {
            targetStatus = OrderStatus.CANCELLED;
            syncRemarks = "All seller sub-orders cancelled. Parent order marked CANCELLED.";
        } else if (anyShipped && order.getStatus() != OrderStatus.SHIPPED && order.getStatus() != OrderStatus.DELIVERED) {
            targetStatus = OrderStatus.SHIPPED;
            syncRemarks = "One or more seller sub-orders shipped. Parent order marked SHIPPED.";
        } else if (anyProcessing && (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED)) {
            targetStatus = OrderStatus.PROCESSING;
            syncRemarks = "Seller sub-orders in processing. Parent order marked PROCESSING.";
        } else if (allConfirmedOrBeyond && order.getStatus() == OrderStatus.PENDING) {
            targetStatus = OrderStatus.CONFIRMED;
            syncRemarks = "All seller sub-orders confirmed. Parent order marked CONFIRMED.";
        }

        if (targetStatus != null && targetStatus != order.getStatus()) {
            OrderStatus current = order.getStatus();
            Set<OrderStatus> allowed = VALID_ORDER_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(OrderStatus.class));
            if (allowed.contains(targetStatus)) {
                order.setStatus(targetStatus);
                order.setUpdatedBy(actor);
                order.setUpdatedAt(Instant.now());
                if (targetStatus == OrderStatus.CANCELLED && order.getCancelledAt() == null) {
                    order.setCancelledAt(Instant.now());
                    order.setCancellationReason(syncRemarks);
                }
                orderRepository.save(order);
                recordHistory(order, null, current.name(), targetStatus.name(), syncRemarks, actor);
                log.info("[PARENT ORDER SYNC] Order {} automatically updated to {} ({})",
                        order.getOrderNumber(), targetStatus, syncRemarks);
            }
        }
    }

    /**
     * Records an immutable status transition in `order_status_history`.
     */
    @Transactional
    public OrderStatusHistoryEntity recordHistory(OrderEntity order,
                                                  SellerOrderEntity sellerOrder,
                                                  String previousStatus,
                                                  String newStatus,
                                                  String remarks,
                                                  UserEntity changedBy) {
        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .order(order)
                .sellerOrder(sellerOrder)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .remarks(remarks)
                .changedBy(changedBy)
                .createdAt(Instant.now())
                .build();

        return historyRepository.save(history);
    }
}
