package com.bazaarhub.backend.order.application.service;

import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.UpdateOrderStatusRequest;
import com.bazaarhub.backend.order.application.dto.response.SellerOrderResponse;
import com.bazaarhub.backend.order.application.mapper.OrderMapper;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException;
import com.bazaarhub.backend.order.domain.exception.SellerOrderNotFoundException;
import com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Service managing merchant vendor sub-orders, state updates, fulfillment,
 * and vendor-initiated sub-order cancellations.
 */
@Service
public class SellerOrderService {

    private static final Logger log = LoggerFactory.getLogger(SellerOrderService.class);

    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;
    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final StockReservationService stockReservationService;
    private final SpringDataInventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public SellerOrderService(SpringDataSellerOrderRepository sellerOrderRepository,
                              SpringDataOrderItemRepository orderItemRepository,
                              SpringDataOrderStatusHistoryRepository historyRepository,
                              SpringDataSellerRepository sellerRepository,
                              SpringDataUserRepository userRepository,
                              OrderStatusTransitionService orderStatusTransitionService,
                              StockReservationService stockReservationService,
                              SpringDataInventoryRepository inventoryRepository,
                              InventoryService inventoryService) {
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.stockReservationService = stockReservationService;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Seller lists their own vendor sub-orders with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<SellerOrderResponse> getSellerOrders(Long userId, SellerOrderStatus status, Pageable pageable) {
        SellerEntity seller = resolveApprovedSeller(userId);

        Page<SellerOrderEntity> orders;
        if (status != null) {
            orders = sellerOrderRepository.findBySellerIdAndStatus(seller.getId(), status, pageable);
        } else {
            orders = sellerOrderRepository.findBySellerId(seller.getId(), pageable);
        }

        return orders.map(so -> {
            List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(so.getId());
            return OrderMapper.toSellerOrderResponse(so, items, Collections.emptyList());
        });
    }

    /**
     * Seller retrieves details of a specific sub-order.
     */
    @Transactional(readOnly = true)
    public SellerOrderResponse getSellerOrderById(Long userId, Long sellerOrderId) {
        SellerEntity seller = resolveApprovedSeller(userId);
        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new SellerOrderNotFoundException(sellerOrderId));

        validateSellerOwnership(sellerOrder, seller);

        return loadSellerOrderDetails(sellerOrder);
    }

    /**
     * Seller retrieves details of a specific sub-order by sub-order number.
     */
    @Transactional(readOnly = true)
    public SellerOrderResponse getSellerOrderBySubOrderNumber(Long userId, String subOrderNumber) {
        SellerEntity seller = resolveApprovedSeller(userId);
        SellerOrderEntity sellerOrder = sellerOrderRepository.findBySubOrderNumber(subOrderNumber)
                .orElseThrow(() -> new SellerOrderNotFoundException("subOrderNumber", subOrderNumber));

        validateSellerOwnership(sellerOrder, seller);

        return loadSellerOrderDetails(sellerOrder);
    }

    /**
     * Seller updates the status of their sub-order (e.g. PENDING -> CONFIRMED -> PROCESSING -> READY_FOR_PICKUP -> SHIPPED -> DELIVERED).
     */
    @Transactional
    public SellerOrderResponse updateSellerOrderStatus(Long userId, Long sellerOrderId, UpdateOrderStatusRequest request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new InvalidOrderOperationException("Status is required");
        }

        SellerEntity seller = resolveApprovedSeller(userId);
        UserEntity user = resolveUser(userId);
        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new SellerOrderNotFoundException(sellerOrderId));

        validateSellerOwnership(sellerOrder, seller);

        SellerOrderStatus targetStatus;
        try {
            targetStatus = SellerOrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrderOperationException("Invalid seller order status: " + request.getStatus());
        }

        // If transitioning to CANCELLED, execute cancellation logic
        if (targetStatus == SellerOrderStatus.CANCELLED) {
            return cancelSellerOrder(userId, sellerOrderId, CancelOrderRequest.builder().reason(request.getRemarks()).build());
        }

        // If transitioning to CONFIRMED, confirm active stock reservation (permanently deduct stock)
        if (targetStatus == SellerOrderStatus.CONFIRMED && sellerOrder.getStatus() == SellerOrderStatus.PENDING) {
            List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(sellerOrder.getId());
            for (OrderItemEntity item : items) {
                try {
                    stockReservationService.confirmReservation(
                            sellerOrder.getOrder().getOrderNumber(), item.getVariant().getId(), userId);
                } catch (Exception ex) {
                    log.warn("[SELLER ORDER CONFIRM] Reservation confirm note for variant {}: {}",
                            item.getVariant().getId(), ex.getMessage());
                }
            }
        }

        SellerOrderEntity updated = orderStatusTransitionService.transitionSellerOrderStatus(
                sellerOrder, targetStatus, request.getRemarks(), user);

        log.info("[SELLER ORDER UPDATED] Seller ID: {} updated sub-order ID: {} to {}",
                seller.getId(), sellerOrderId, targetStatus);

        return loadSellerOrderDetails(updated);
    }

    /**
     * Seller cancels their sub-order before shipment.
     * Restores/releases stock for the cancelled items.
     */
    @Transactional
    public SellerOrderResponse cancelSellerOrder(Long userId, Long sellerOrderId, CancelOrderRequest request) {
        SellerEntity seller = resolveApprovedSeller(userId);
        UserEntity user = resolveUser(userId);
        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new SellerOrderNotFoundException(sellerOrderId));

        validateSellerOwnership(sellerOrder, seller);

        if (sellerOrder.getStatus() == SellerOrderStatus.SHIPPED ||
                sellerOrder.getStatus() == SellerOrderStatus.DELIVERED ||
                sellerOrder.getStatus() == SellerOrderStatus.CANCELLED ||
                sellerOrder.getStatus() == SellerOrderStatus.RETURNED) {
            throw new InvalidOrderOperationException(
                    "Cannot cancel sub-order in status: " + sellerOrder.getStatus());
        }

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Cancelled by seller";

        // 1. Transition sub-order to CANCELLED
        SellerOrderEntity cancelled = orderStatusTransitionService.transitionSellerOrderStatus(
                sellerOrder, SellerOrderStatus.CANCELLED, reason, user);

        // 2. Release / restock items
        List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(sellerOrder.getId());
        for (OrderItemEntity item : items) {
            try {
                stockReservationService.releaseReservation(
                        sellerOrder.getOrder().getOrderNumber(), item.getVariant().getId(), userId, "Seller cancelled sub-order: " + reason);
            } catch (Exception ex) {
                log.warn("[SELLER CANCEL RESTOCK] Releasing reservation failed ({}), attempting return for variant {}: ",
                        ex.getMessage(), item.getVariant().getId());
                inventoryRepository.findByVariantId(item.getVariant().getId()).ifPresent(inv -> {
                    inv.setTotalQuantity(inv.getTotalQuantity() + item.getQuantity());
                    inventoryRepository.save(inv);
                    inventoryService.recordMovement(inv, MovementType.RETURN, item.getQuantity(),
                            "SELLER_SUB_ORDER_CANCELLED", sellerOrder.getId(),
                            "Restocked upon seller cancellation: " + sellerOrder.getSubOrderNumber(), user);
                });
            }
        }

        log.info("[SELLER ORDER CANCELLED] Seller ID: {} cancelled sub-order ID: {} ({})",
                seller.getId(), sellerOrderId, reason);

        return loadSellerOrderDetails(cancelled);
    }

    private SellerOrderResponse loadSellerOrderDetails(SellerOrderEntity sellerOrder) {
        List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(sellerOrder.getId());
        List<OrderStatusHistoryEntity> history = historyRepository.findBySellerOrderIdOrderByCreatedAtDesc(sellerOrder.getId());
        return OrderMapper.toSellerOrderResponse(sellerOrder, items, history);
    }

    private void validateSellerOwnership(SellerOrderEntity sellerOrder, SellerEntity seller) {
        if (!sellerOrder.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedOrderAccessException("You do not have permission to view or manage this seller order.");
        }
    }

    private SellerEntity resolveApprovedSeller(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedOrderAccessException("No seller profile found for user ID: " + userId));

        if (seller.getApprovalStatus() != SellerApprovalStatus.APPROVED) {
            throw new UnauthorizedOrderAccessException("Seller account is not approved. Current status: " + seller.getApprovalStatus());
        }

        return seller;
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
