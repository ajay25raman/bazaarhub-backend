package com.bazaarhub.backend.order.application.service;

import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.application.dto.response.OrderResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderSummaryResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderStatusHistoryResponse;
import com.bazaarhub.backend.order.application.dto.response.SellerOrderResponse;
import com.bazaarhub.backend.order.application.mapper.OrderMapper;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException;
import com.bazaarhub.backend.order.domain.exception.OrderNotFoundException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing administrative order oversight, multi-tenant order queries,
 * administrative status overrides, and system-wide audit tracking.
 */
@Service
public class AdminOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderService.class);

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;
    private final SpringDataUserRepository userRepository;
    private final OrderService orderService;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final StockReservationService stockReservationService;
    private final SpringDataInventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public AdminOrderService(SpringDataOrderRepository orderRepository,
                             SpringDataSellerOrderRepository sellerOrderRepository,
                             SpringDataOrderItemRepository orderItemRepository,
                             SpringDataOrderStatusHistoryRepository historyRepository,
                             SpringDataUserRepository userRepository,
                             OrderService orderService,
                             OrderStatusTransitionService orderStatusTransitionService,
                             StockReservationService stockReservationService,
                             SpringDataInventoryRepository inventoryRepository,
                             InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.stockReservationService = stockReservationService;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Admin searches and lists all orders with flexible filters (status, userId).
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getAllOrders(OrderStatus status, Long userId, Pageable pageable) {
        Specification<OrderEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(OrderMapper::toOrderSummaryResponse);
    }

    /**
     * Admin views complete order tree.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return orderService.getOrderDetails(orderId);
    }

    /**
     * Admin updates parent order status with remarks.
     * If transitioning to CANCELLED, cascades cancellation and stock release.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long adminUserId) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new InvalidOrderOperationException("Status is required");
        }

        UserEntity admin = resolveUser(adminUserId);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus targetStatus;
        try {
            targetStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderOperationException("Invalid order status: " + request.getStatus());
        }

        // If target status is CANCELLED, execute cancellation and stock release
        if (targetStatus == OrderStatus.CANCELLED) {
            orderStatusTransitionService.transitionOrderStatus(order, OrderStatus.CANCELLED, request.getRemarks(), admin);

            List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(order.getId());
            for (SellerOrderEntity so : sellerOrders) {
                if (so.getStatus() != SellerOrderStatus.CANCELLED && so.getStatus() != SellerOrderStatus.DELIVERED) {
                    orderStatusTransitionService.transitionSellerOrderStatus(
                            so, SellerOrderStatus.CANCELLED, request.getRemarks(), admin);
                }
                List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(so.getId());
                for (OrderItemEntity item : items) {
                    try {
                        stockReservationService.releaseReservation(
                                order.getOrderNumber(), item.getVariant().getId(), adminUserId, "Admin cancelled order: " + request.getRemarks());
                    } catch (Exception ex) {
                        inventoryRepository.findByVariantId(item.getVariant().getId()).ifPresent(inv -> {
                            inv.setTotalQuantity(inv.getTotalQuantity() + item.getQuantity());
                            inventoryRepository.save(inv);
                            inventoryService.recordMovement(inv, MovementType.RETURN, item.getQuantity(),
                                    "ADMIN_ORDER_CANCELLED", order.getId(),
                                    "Restocked upon admin cancellation: " + order.getOrderNumber(), admin);
                        });
                    }
                }
            }
        } else {
            orderStatusTransitionService.transitionOrderStatus(order, targetStatus, request.getRemarks(), admin);
        }

        log.info("[ADMIN ORDER STATUS UPDATE] Admin ID: {} changed order ID: {} to {}",
                adminUserId, orderId, targetStatus);

        return orderService.getOrderDetails(order.getId());
    }

    /**
     * Admin searches and lists all seller sub-orders across vendors.
     */
    @Transactional(readOnly = true)
    public Page<SellerOrderResponse> getAllSellerOrders(SellerOrderStatus status, Long sellerId, Pageable pageable) {
        Specification<SellerOrderEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return sellerOrderRepository.findAll(spec, pageable).map(so -> {
            List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(so.getId());
            return OrderMapper.toSellerOrderResponse(so, items, java.util.Collections.emptyList());
        });
    }

    /**
     * Admin retrieves status transition history for an order.
     */
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(Long orderId) {
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        return historyRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(OrderMapper::toOrderStatusHistoryResponse)
                .toList();
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found with ID: " + userId));
    }
}
