package com.bazaarhub.backend.order.application.service;

import com.bazaarhub.backend.cart.application.service.CartService;
import com.bazaarhub.backend.cart.domain.model.CartStatus;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.bazaarhub.backend.cart.infrastructure.persistence.repository.SpringDataCartRepository;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductApprovalStatus;
import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
import com.bazaarhub.backend.customer.domain.exception.AddressNotFoundException;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.inventory.application.dto.request.StockReserveRequest;
import com.bazaarhub.backend.inventory.application.service.InventoryService;
import com.bazaarhub.backend.inventory.application.service.StockReservationService;
import com.bazaarhub.backend.inventory.domain.exception.InsufficientStockException;
import com.bazaarhub.backend.inventory.domain.model.MovementType;
import com.bazaarhub.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.bazaarhub.backend.inventory.infrastructure.persistence.repository.SpringDataInventoryRepository;
import com.bazaarhub.backend.order.application.dto.request.CancelOrderRequest;
import com.bazaarhub.backend.order.application.dto.request.CreateOrderRequest;
import com.bazaarhub.backend.order.application.dto.response.OrderResponse;
import com.bazaarhub.backend.order.application.dto.response.OrderSummaryResponse;
import com.bazaarhub.backend.order.application.mapper.OrderMapper;
import com.bazaarhub.backend.order.domain.exception.InvalidOrderOperationException;
import com.bazaarhub.backend.order.domain.exception.OrderNotFoundException;
import com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderItemRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderStatusHistoryRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service managing order checkout creation, customer order queries,
 * and order cancellation workflows.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataOrderStatusHistoryRepository historyRepository;
    private final CartService cartService;
    private final SpringDataCartRepository cartRepository;
    private final SpringDataAddressRepository addressRepository;
    private final SpringDataUserRepository userRepository;
    private final StockReservationService stockReservationService;
    private final SpringDataInventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final OrderStatusTransitionService orderStatusTransitionService;

    public OrderService(SpringDataOrderRepository orderRepository,
                        SpringDataSellerOrderRepository sellerOrderRepository,
                        SpringDataOrderItemRepository orderItemRepository,
                        SpringDataOrderStatusHistoryRepository historyRepository,
                        CartService cartService,
                        SpringDataCartRepository cartRepository,
                        SpringDataAddressRepository addressRepository,
                        SpringDataUserRepository userRepository,
                        StockReservationService stockReservationService,
                        SpringDataInventoryRepository inventoryRepository,
                        InventoryService inventoryService,
                        OrderStatusTransitionService orderStatusTransitionService) {
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.historyRepository = historyRepository;
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.stockReservationService = stockReservationService;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
        this.orderStatusTransitionService = orderStatusTransitionService;
    }

    /**
     * Places a customer order from their current active cart.
     * Validates address, stock, pricing, executes stock reservation, splits orders
     * across merchants, and records audit history.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        UserEntity user = resolveUser(userId);

        // 1. Fetch active cart and validate non-empty
        CartEntity cart = cartService.getOrCreateCart(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidOrderOperationException("Cart is empty. Add items before placing an order.");
        }

        // 2. Validate and resolve delivery address
        AddressEntity address;
        if (request != null && request.getAddressId() != null) {
            address = addressRepository.findById(request.getAddressId())
                    .orElseThrow(() -> new AddressNotFoundException(request.getAddressId()));
            if (!address.getUser().getId().equals(userId)) {
                throw new UnauthorizedAddressAccessException("Cannot use delivery address belonging to another customer");
            }
        } else {
            address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                    .orElseThrow(() -> new InvalidOrderOperationException("No delivery address selected and customer has no default address configured."));
        }

        // 3. Pre-validate all cart items for active/approved status and inventory availability
        for (CartItemEntity item : cart.getItems()) {
            if (item.getVariant() == null || item.getVariant().getProduct() == null) {
                throw new InvalidOrderOperationException("Cart contains invalid product variant references.");
            }
            if (item.getVariant().getStatus() != CatalogStatus.ACTIVE) {
                throw new InvalidOrderOperationException("Variant '" + item.getVariant().getSku() + "' is inactive.");
            }
            if (item.getVariant().getProduct().getStatus() != ProductStatus.ACTIVE ||
                    item.getVariant().getProduct().getApprovalStatus() != ProductApprovalStatus.APPROVED) {
                throw new InvalidOrderOperationException("Product '" + item.getVariant().getProduct().getName() + "' is not approved or active.");
            }
            SellerEntity seller = item.getVariant().getProduct().getSeller();
            if (seller == null || seller.getApprovalStatus() != SellerApprovalStatus.APPROVED) {
                throw new InvalidOrderOperationException("Merchant store is not approved for retail fulfillment.");
            }

            int availableStock = cartService.getAvailableStock(item.getVariant().getId());
            if (availableStock < item.getQuantity()) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock for SKU '%s'. Requested: %d, Available: %d",
                        item.getVariant().getSku(), item.getQuantity(), availableStock));
            }
        }

        // 4. Generate unique parent order number
        String orderNumber = generateUniqueOrderNumber();

        // 5. Reserve stock for each item line under this order reference
        List<Long> reservedVariantIds = new ArrayList<>();
        try {
            for (CartItemEntity item : cart.getItems()) {
                StockReserveRequest reserveReq = StockReserveRequest.builder()
                        .checkoutReference(orderNumber)
                        .variantId(item.getVariant().getId())
                        .quantity(item.getQuantity())
                        .ttlMinutes(30)
                        .build();
                stockReservationService.reserveStock(reserveReq, userId);
                reservedVariantIds.add(item.getVariant().getId());
            }
        } catch (Exception ex) {
            log.error("[ORDER CREATION] Stock reservation failed for order {}: {}. Rolling back reservations.",
                    orderNumber, ex.getMessage());
            for (Long varId : reservedVariantIds) {
                try {
                    stockReservationService.releaseReservation(orderNumber, varId, userId, "Order placement failure rollback");
                } catch (Exception rollbackEx) {
                    log.warn("[ORDER CREATION ROLLBACK ERROR] Could not release reservation for variant {}: {}",
                            varId, rollbackEx.getMessage());
                }
            }
            throw ex;
        }

        // 6. Group items by Merchant Seller
        Map<SellerEntity, List<CartItemEntity>> sellerItemsMap = cart.getItems().stream()
                .collect(Collectors.groupingBy(item -> item.getVariant().getProduct().getSeller()));

        // 7. Calculate overall order financial totals
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (CartItemEntity item : cart.getItems()) {
            BigDecimal price = item.getVariant().getSellingPrice();
            BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxRate = item.getVariant().getTaxRate() != null ? item.getVariant().getTaxRate() : BigDecimal.ZERO;
            BigDecimal itemTax = itemTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(itemTotal);
            taxAmount = taxAmount.add(itemTax);
        }

        BigDecimal deliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subtotal.add(taxAmount).add(deliveryFee).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        // 8. Build and persist parent OrderEntity
        String recipientName = address.getRecipientName() != null ? address.getRecipientName() : user.getFullName();
        String recipientPhone = address.getPhone() != null ? address.getPhone() : (user.getPhone() != null ? user.getPhone() : "0000000000");

        OrderEntity order = OrderEntity.builder()
                .orderNumber(orderNumber)
                .user(user)
                .shippingAddress(address)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .addressLine1(address.getAddressLine1() != null ? address.getAddressLine1() : "N/A")
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity() != null ? address.getCity() : "N/A")
                .state(address.getState() != null ? address.getState() : "N/A")
                .postalCode(address.getPostalCode() != null ? address.getPostalCode() : "000000")
                .country(address.getCountry() != null ? address.getCountry() : "India")
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .taxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP))
                .deliveryFee(deliveryFee)
                .discountAmount(discountAmount)
                .grandTotal(grandTotal)
                .specialInstructions(request != null ? request.getSpecialInstructions() : null)
                .placedAt(Instant.now())
                .createdBy(user)
                .updatedBy(user)
                .build();

        OrderEntity savedOrder = orderRepository.save(order);

        // Record parent order status history
        orderStatusTransitionService.recordHistory(
                savedOrder, null, null, OrderStatus.PENDING.name(), "Order placed successfully by customer", user);

        // 9. Split and create SellerOrderEntity and OrderItemEntity per vendor
        for (Map.Entry<SellerEntity, List<CartItemEntity>> entry : sellerItemsMap.entrySet()) {
            SellerEntity seller = entry.getKey();
            List<CartItemEntity> sellerItems = entry.getValue();

            BigDecimal sellerSubtotal = BigDecimal.ZERO;
            BigDecimal sellerTax = BigDecimal.ZERO;

            for (CartItemEntity item : sellerItems) {
                BigDecimal price = item.getVariant().getSellingPrice();
                BigDecimal itemTotal = price.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                BigDecimal taxRate = item.getVariant().getTaxRate() != null ? item.getVariant().getTaxRate() : BigDecimal.ZERO;
                BigDecimal itemTax = itemTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                sellerSubtotal = sellerSubtotal.add(itemTotal);
                sellerTax = sellerTax.add(itemTax);
            }

            BigDecimal sellerDeliveryFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellerTotal = sellerSubtotal.add(sellerTax).add(sellerDeliveryFee).setScale(2, RoundingMode.HALF_UP);

            String subOrderNumber = generateSubOrderNumber(orderNumber, seller.getId());

            SellerOrderEntity sellerOrder = SellerOrderEntity.builder()
                    .order(savedOrder)
                    .seller(seller)
                    .subOrderNumber(subOrderNumber)
                    .status(SellerOrderStatus.PENDING)
                    .subtotal(sellerSubtotal.setScale(2, RoundingMode.HALF_UP))
                    .taxAmount(sellerTax.setScale(2, RoundingMode.HALF_UP))
                    .deliveryFee(sellerDeliveryFee)
                    .commissionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalAmount(sellerTotal)
                    .createdBy(user)
                    .updatedBy(user)
                    .build();

            SellerOrderEntity savedSellerOrder = sellerOrderRepository.save(sellerOrder);

            // Record sub-order creation history
            orderStatusTransitionService.recordHistory(
                    savedOrder, savedSellerOrder, null, SellerOrderStatus.PENDING.name(),
                    "Sub-order created for vendor: " + seller.getBusinessName(), user);

            // Create OrderItem entities
            for (CartItemEntity item : sellerItems) {
                BigDecimal unitPrice = item.getVariant().getSellingPrice();
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                BigDecimal taxRate = item.getVariant().getTaxRate() != null ? item.getVariant().getTaxRate() : BigDecimal.ZERO;
                BigDecimal itemTax = itemTotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                OrderItemEntity orderItem = OrderItemEntity.builder()
                        .sellerOrder(savedSellerOrder)
                        .product(item.getVariant().getProduct())
                        .variant(item.getVariant())
                        .productName(item.getVariant().getProduct().getName())
                        .variantSku(item.getVariant().getSku())
                        .variantAttributes(item.getVariant().getAttributes())
                        .unitPrice(unitPrice)
                        .mrp(item.getVariant().getMrp())
                        .taxRate(taxRate)
                        .taxAmount(itemTax)
                        .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                        .quantity(item.getQuantity())
                        .totalPrice(itemTotal)
                        .createdBy(user)
                        .updatedBy(user)
                        .build();

                orderItemRepository.save(orderItem);
                savedSellerOrder.getItems().add(orderItem);
            }

            savedOrder.addSellerOrder(savedSellerOrder);
        }

        // 10. Update cart status to CONVERTED and clear items
        cart.setStatus(CartStatus.CONVERTED);
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("[ORDER CREATED] Order ID: {}, Order Number: {}, Customer: {}, Total: INR {}",
                savedOrder.getId(), savedOrder.getOrderNumber(), user.getEmail(), savedOrder.getGrandTotal());

        return getOrderDetails(savedOrder.getId());
    }

    /**
     * Customer retrieves their paginated order history with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getCustomerOrders(Long userId, OrderStatus status, Pageable pageable) {
        if (status != null) {
            return orderRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(OrderMapper::toOrderSummaryResponse);
        }
        return orderRepository.findByUserId(userId, pageable)
                .map(OrderMapper::toOrderSummaryResponse);
    }

    /**
     * Customer retrieves detailed order information by order ID.
     */
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderById(Long userId, Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOrderAccessException("Cannot access order belonging to another customer");
        }

        return getOrderDetails(order.getId());
    }

    /**
     * Customer retrieves detailed order information by unique order number.
     */
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderByNumber(Long userId, String orderNumber) {
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("orderNumber", orderNumber));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOrderAccessException("Cannot access order belonging to another customer");
        }

        return getOrderDetails(order.getId());
    }

    /**
     * Customer cancels an order before shipment.
     * Can only cancel if order status is PENDING or CONFIRMED.
     * Releases reserved warehouse stock and logs audit history.
     */
    @Transactional
    public OrderResponse cancelCustomerOrder(Long userId, Long orderId, CancelOrderRequest request) {
        UserEntity user = resolveUser(userId);
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOrderAccessException("Cannot cancel order belonging to another customer");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderOperationException(
                    "Order cannot be cancelled in status: " + order.getStatus() + ". Only PENDING or CONFIRMED orders are cancellable.");
        }

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Cancelled by customer";

        // 1. Transition parent order to CANCELLED
        orderStatusTransitionService.transitionOrderStatus(order, OrderStatus.CANCELLED, reason, user);

        // 2. Cascade cancellation to all non-terminal seller sub-orders
        List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(order.getId());
        for (SellerOrderEntity so : sellerOrders) {
            if (so.getStatus() != SellerOrderStatus.CANCELLED && so.getStatus() != SellerOrderStatus.DELIVERED) {
                orderStatusTransitionService.transitionSellerOrderStatus(so, SellerOrderStatus.CANCELLED, reason, user);
            }
        }

        // 3. Release reserved warehouse inventory for all items
        for (SellerOrderEntity so : sellerOrders) {
            List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(so.getId());
            for (OrderItemEntity item : items) {
                try {
                    stockReservationService.releaseReservation(
                            order.getOrderNumber(), item.getVariant().getId(), userId, "Customer cancelled order: " + reason);
                } catch (Exception ex) {
                    log.warn("[ORDER CANCELLATION RESTOCK] Releasing active reservation failed ({}), attempting restock for variant ID: {}",
                            ex.getMessage(), item.getVariant().getId());
                    inventoryRepository.findByVariantId(item.getVariant().getId()).ifPresent(inv -> {
                        inv.setTotalQuantity(inv.getTotalQuantity() + item.getQuantity());
                        inventoryRepository.save(inv);
                        inventoryService.recordMovement(inv, MovementType.RETURN, item.getQuantity(),
                                "ORDER_CANCELLED", order.getId(),
                                "Restocked upon customer order cancellation: " + order.getOrderNumber(), user);
                    });
                }
            }
        }

        log.info("[ORDER CANCELLED] Customer ID: {} cancelled order ID: {} ({})", userId, orderId, reason);

        return getOrderDetails(order.getId());
    }

    /**
     * Internal helper to load full order tree with history.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<OrderStatusHistoryEntity> orderHistory = historyRepository.findByOrderIdOrderByCreatedAtDesc(orderId);

        List<SellerOrderEntity> sellerOrders = sellerOrderRepository.findByOrderId(orderId);
        Map<Long, List<OrderItemEntity>> sellerItemsMap = new HashMap<>();
        Map<Long, List<OrderStatusHistoryEntity>> sellerHistoryMap = new HashMap<>();
        for (SellerOrderEntity so : sellerOrders) {
            List<OrderItemEntity> items = orderItemRepository.findBySellerOrderId(so.getId());
            sellerItemsMap.put(so.getId(), items);
            List<OrderStatusHistoryEntity> soHistory = historyRepository.findBySellerOrderIdOrderByCreatedAtDesc(so.getId());
            sellerHistoryMap.put(so.getId(), soHistory);
        }

        return OrderMapper.toOrderResponse(order, sellerOrders, sellerItemsMap, orderHistory, sellerHistoryMap);
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    private String generateUniqueOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNumber;
        do {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                suffix.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            orderNumber = String.format("ORD-%s-%s", datePart, suffix);
        } while (orderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }

    private String generateSubOrderNumber(String orderNumber, Long sellerId) {
        String base = orderNumber.startsWith("ORD-") ? orderNumber.substring(4) : orderNumber;
        return String.format("SUB-%s-S%d", base, sellerId);
    }
}
