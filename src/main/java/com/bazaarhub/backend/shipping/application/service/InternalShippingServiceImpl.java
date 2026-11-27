package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.delivery.infrastructure.adapter.FcmPushNotificationService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.exception.OrderNotFoundException;
import com.bazaarhub.backend.order.domain.exception.SellerOrderNotFoundException;
import com.bazaarhub.backend.order.domain.exception.UnauthorizedOrderAccessException;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shipping.application.dto.request.CancelShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.mapper.ShippingMapper;
import com.bazaarhub.backend.shipping.domain.exception.IneligibleOrderForShipmentException;
import com.bazaarhub.backend.shipping.domain.exception.InvalidShipmentOperationException;
import com.bazaarhub.backend.shipping.domain.exception.ShipmentAlreadyExistsException;
import com.bazaarhub.backend.shipping.domain.exception.UnauthorizedShipmentAccessException;
import com.bazaarhub.backend.shipping.infrastructure.provider.InternalShippingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class InternalShippingServiceImpl implements InternalShippingService {

    private static final Logger log = LoggerFactory.getLogger(InternalShippingServiceImpl.class);

    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataDeliveryAssignmentRepository deliveryAssignmentRepository;
    private final InternalShippingProvider internalShippingProvider;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final FcmPushNotificationService fcmPushNotificationService;
    private final CloudinaryService cloudinaryService;
    private final ShippingMapper shippingMapper;

    public InternalShippingServiceImpl(
            SpringDataShipmentRepository shipmentRepository,
            SpringDataSellerOrderRepository sellerOrderRepository,
            SpringDataOrderRepository orderRepository,
            SpringDataSellerRepository sellerRepository,
            SpringDataUserRepository userRepository,
            SpringDataPaymentRepository paymentRepository,
            SpringDataDeliveryAssignmentRepository deliveryAssignmentRepository,
            InternalShippingProvider internalShippingProvider,
            OrderStatusTransitionService orderStatusTransitionService,
            FcmPushNotificationService fcmPushNotificationService,
            CloudinaryService cloudinaryService,
            ShippingMapper shippingMapper) {
        this.shipmentRepository = shipmentRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.orderRepository = orderRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.internalShippingProvider = internalShippingProvider;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.fcmPushNotificationService = fcmPushNotificationService;
        this.cloudinaryService = cloudinaryService;
        this.shippingMapper = shippingMapper;
    }

    @Override
    @Transactional
    public InternalShipmentResponse createShipment(Long userId, CreateInternalShipmentRequest request, boolean isAdmin) {
        if (request == null || request.getSellerOrderId() == null) {
            throw new InvalidShipmentOperationException("Seller order ID is required to create a shipment");
        }

        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(request.getSellerOrderId())
                .orElseThrow(() -> new SellerOrderNotFoundException(request.getSellerOrderId()));

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // 1. Verify seller ownership if actor is not an admin
        if (!isAdmin) {
            SellerEntity seller = resolveApprovedSeller(userId);
            if (!sellerOrder.getSeller().getId().equals(seller.getId())) {
                throw new UnauthorizedShipmentAccessException("You do not have permission to create a shipment for this seller order");
            }
        }

        // 2. Prevent duplicate shipment creation (strict DB constraint safety)
        if (shipmentRepository.existsBySellerOrderId(sellerOrder.getId())) {
            throw new ShipmentAlreadyExistsException(sellerOrder.getId());
        }

        // 3. Verify order status eligibility
        OrderEntity parentOrder = sellerOrder.getOrder();
        if (parentOrder.getStatus() == OrderStatus.CANCELLED || parentOrder.getStatus() == OrderStatus.RETURNED) {
            throw new IneligibleOrderForShipmentException("Cannot create shipment for order in status: " + parentOrder.getStatus());
        }
        if (sellerOrder.getStatus() == SellerOrderStatus.CANCELLED || sellerOrder.getStatus() == SellerOrderStatus.RETURNED) {
            throw new IneligibleOrderForShipmentException("Cannot create shipment for seller sub-order in status: " + sellerOrder.getStatus());
        }

        // 4. Verify payment eligibility (Prepaid vs COD)
        String paymentMethod = resolvePaymentMethod(parentOrder);
        if (parentOrder.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new IneligibleOrderForShipmentException("Cannot create shipment for order with FAILED payment status");
        }

        // 5. Create internal shipment through InternalShippingProvider
        ShipmentEntity createdShipment = internalShippingProvider.createShipment(sellerOrder, request, actor);

        // 6. Synchronize seller sub-order status (PENDING -> CONFIRMED/PROCESSING)
        if (sellerOrder.getStatus() == SellerOrderStatus.PENDING) {
            orderStatusTransitionService.transitionSellerOrderStatus(
                    sellerOrder, SellerOrderStatus.CONFIRMED, "Shipment created", actor);
        }

        // 7. Dispatch notifications
        sendMilestoneNotification(
                parentOrder.getUser(),
                "SHIPMENT_CREATED",
                "Shipment Dispatched",
                "Shipment " + createdShipment.getShipmentNumber() + " has been initiated for your order.",
                createdShipment.getId());

        log.info("[SHIPMENT CREATED] Successfully created shipment {} for seller order ID: {}",
                createdShipment.getShipmentNumber(), sellerOrder.getId());

        return shippingMapper.toInternalResponse(createdShipment, paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalShipmentResponse getShipmentById(Long userId, Long shipmentId, boolean isAdmin) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        if (!isAdmin && userId != null) {
            validateShipmentAccess(shipment, userId);
        }

        String paymentMethod = resolvePaymentMethod(shipment.getSellerOrder() != null ? shipment.getSellerOrder().getOrder() : null);
        return shippingMapper.toInternalResponse(shipment, paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalShipmentResponse> getShipmentsByOrderId(Long userId, Long orderId, boolean isAdmin) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!isAdmin && userId != null) {
            if (!order.getUser().getId().equals(userId)) {
                throw new UnauthorizedShipmentAccessException("You are not authorized to view shipments for this order");
            }
        }

        String paymentMethod = resolvePaymentMethod(order);
        List<ShipmentEntity> shipments = shipmentRepository.findBySellerOrderOrderId(orderId);
        return shipments.stream()
                .map(s -> shippingMapper.toInternalResponse(s, paymentMethod))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalShipmentResponse> getSellerShipments(Long userId, ShipmentStatus status, Pageable pageable) {
        SellerEntity seller = resolveApprovedSeller(userId);

        Page<ShipmentEntity> page;
        if (status != null) {
            page = shipmentRepository.findBySellerOrderSellerIdAndStatus(seller.getId(), status, pageable);
        } else {
            page = shipmentRepository.findBySellerOrderSellerId(seller.getId(), pageable);
        }

        return page.map(s -> shippingMapper.toInternalResponse(s, resolvePaymentMethod(s.getSellerOrder() != null ? s.getSellerOrder().getOrder() : null)));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalShipmentResponse getSellerShipmentById(Long userId, Long shipmentId) {
        SellerEntity seller = resolveApprovedSeller(userId);
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        if (shipment.getSellerOrder() == null || !shipment.getSellerOrder().getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedShipmentAccessException("You do not have permission to view this shipment");
        }

        String paymentMethod = resolvePaymentMethod(shipment.getSellerOrder().getOrder());
        return shippingMapper.toInternalResponse(shipment, paymentMethod);
    }

    @Override
    @Transactional
    public InternalShipmentResponse markReadyForPickup(Long userId, Long shipmentId, ReadyForPickupRequest request) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Validate seller ownership
        SellerEntity seller = resolveApprovedSeller(userId);
        if (shipment.getSellerOrder() == null || !shipment.getSellerOrder().getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedShipmentAccessException("You do not have permission to update this shipment");
        }

        if (shipment.getStatus() != ShipmentStatus.PENDING && shipment.getStatus() != ShipmentStatus.FAILED) {
            throw new InvalidShipmentOperationException("Cannot mark ready for pickup. Current status: " + shipment.getStatus());
        }

        // Upload packaging photo to Cloudinary if provided
        String photoUrl = null;
        if (request != null && request.getPackagingPhotoUrl() != null && !request.getPackagingPhotoUrl().isBlank()) {
            photoUrl = cloudinaryService.uploadImage(request.getPackagingPhotoUrl(), "bazaarhub/shipping/parcels");
        }

        // Encode package info into delivery notes
        String encodedNotes = shippingMapper.encodePackageInfo(
                request != null ? request.getPackageWeight() : null,
                request != null ? request.getLengthCm() : null,
                request != null ? request.getWidthCm() : null,
                request != null ? request.getHeightCm() : null,
                request != null ? request.getIsFragile() : null,
                request != null ? request.getNotes() : null,
                photoUrl);

        shipment.setStatus(ShipmentStatus.READY_FOR_PICKUP);
        shipment.setDeliveryNotes(encodedNotes);
        shipment.setUpdatedBy(actor);
        shipment.setUpdatedAt(Instant.now());
        ShipmentEntity saved = shipmentRepository.save(shipment);

        // Transition seller order to PROCESSING if needed
        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        if (sellerOrder != null && sellerOrder.getStatus() != SellerOrderStatus.PROCESSING) {
            orderStatusTransitionService.transitionSellerOrderStatus(
                    sellerOrder, SellerOrderStatus.PROCESSING, "Parcel packaged and ready for pickup", actor);
        }

        // Dispatch notifications
        if (sellerOrder != null && sellerOrder.getOrder() != null) {
            sendMilestoneNotification(
                    sellerOrder.getOrder().getUser(),
                    "SHIPMENT_READY_FOR_PICKUP",
                    "Order Packaged",
                    "Your items for shipment " + saved.getShipmentNumber() + " are packaged and waiting for courier pickup.",
                    saved.getId());
        }

        log.info("[READY FOR PICKUP] Shipment {} marked READY_FOR_PICKUP by seller ID: {}",
                saved.getShipmentNumber(), seller.getId());

        String paymentMethod = resolvePaymentMethod(sellerOrder != null ? sellerOrder.getOrder() : null);
        return shippingMapper.toInternalResponse(saved, paymentMethod);
    }

    @Override
    @Transactional
    public InternalShipmentResponse cancelShipment(Long userId, Long shipmentId, CancelShipmentRequest request, boolean isAdmin) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        UserEntity actor = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        if (!isAdmin) {
            SellerEntity seller = resolveApprovedSeller(userId);
            if (shipment.getSellerOrder() == null || !shipment.getSellerOrder().getSeller().getId().equals(seller.getId())) {
                throw new UnauthorizedShipmentAccessException("You do not have permission to cancel this shipment");
            }
            if (shipment.getStatus() != ShipmentStatus.PENDING && shipment.getStatus() != ShipmentStatus.READY_FOR_PICKUP) {
                throw new InvalidShipmentOperationException("Seller can only cancel shipment before pickup. Current status: " + shipment.getStatus());
            }
        }

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Cancelled by administrator";

        // Cancel active delivery assignment if one exists
        Optional<DeliveryAssignmentEntity> activeAssignment = deliveryAssignmentRepository
                .findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(shipmentId, DeliveryAssignmentStatus.CANCELLED);
        if (activeAssignment.isPresent()) {
            DeliveryAssignmentEntity assignment = activeAssignment.get();
            assignment.setAssignmentStatus(DeliveryAssignmentStatus.CANCELLED);
            assignment.setRejectionReason("Shipment cancelled: " + reason);
            assignment.setUpdatedBy(actor);
            assignment.setUpdatedAt(Instant.now());
            deliveryAssignmentRepository.save(assignment);
        }

        ShipmentEntity cancelled = internalShippingProvider.cancelShipment(shipment, reason, actor);

        // Record history
        SellerOrderEntity sellerOrder = cancelled.getSellerOrder();
        if (sellerOrder != null) {
            orderStatusTransitionService.recordHistory(
                    sellerOrder.getOrder(), sellerOrder,
                    sellerOrder.getStatus().name(), sellerOrder.getStatus().name(),
                    "Shipment cancelled: " + reason, actor);
        }

        // Notify customer
        if (sellerOrder != null && sellerOrder.getOrder() != null) {
            sendMilestoneNotification(
                    sellerOrder.getOrder().getUser(),
                    "SHIPMENT_CANCELLED",
                    "Shipment Cancelled",
                    "Shipment " + cancelled.getShipmentNumber() + " was cancelled. Reason: " + reason,
                    cancelled.getId());
        }

        String paymentMethod = resolvePaymentMethod(sellerOrder != null ? sellerOrder.getOrder() : null);
        return shippingMapper.toInternalResponse(cancelled, paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalShipmentResponse> getCustomerShipments(Long customerId, Pageable pageable) {
        return shipmentRepository.findByCustomerId(customerId, pageable)
                .map(s -> shippingMapper.toInternalResponse(s, resolvePaymentMethod(s.getSellerOrder() != null ? s.getSellerOrder().getOrder() : null)));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalShipmentResponse getCustomerShipmentById(Long customerId, Long shipmentId) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

        if (shipment.getSellerOrder() == null || shipment.getSellerOrder().getOrder() == null
                || !shipment.getSellerOrder().getOrder().getUser().getId().equals(customerId)) {
            throw new UnauthorizedShipmentAccessException("You do not have permission to view this customer shipment");
        }

        String paymentMethod = resolvePaymentMethod(shipment.getSellerOrder().getOrder());
        return shippingMapper.toInternalResponse(shipment, paymentMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalShipmentResponse> adminListShipments(
            String search, ShipmentStatus status, Long sellerId, Long customerId, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return shipmentRepository.adminSearchShipments(cleanSearch, status, sellerId, customerId, pageable)
                .map(s -> shippingMapper.toInternalResponse(s, resolvePaymentMethod(s.getSellerOrder() != null ? s.getSellerOrder().getOrder() : null)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalShipmentResponse> adminListActiveShipments(Pageable pageable) {
        List<ShipmentStatus> activeStatuses = List.of(
                ShipmentStatus.READY_FOR_PICKUP,
                ShipmentStatus.PICKED_UP,
                ShipmentStatus.IN_TRANSIT,
                ShipmentStatus.OUT_FOR_DELIVERY
        );
        return shipmentRepository.findByStatusIn(activeStatuses, pageable)
                .map(s -> shippingMapper.toInternalResponse(s, resolvePaymentMethod(s.getSellerOrder() != null ? s.getSellerOrder().getOrder() : null)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternalShipmentResponse> adminListFailedShipments(Pageable pageable) {
        return shipmentRepository.findByStatus(ShipmentStatus.FAILED, pageable)
                .map(s -> shippingMapper.toInternalResponse(s, resolvePaymentMethod(s.getSellerOrder() != null ? s.getSellerOrder().getOrder() : null)));
    }

    private void validateShipmentAccess(ShipmentEntity shipment, Long userId) {
        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;
        SellerEntity seller = sellerOrder != null ? sellerOrder.getSeller() : null;

        boolean isCustomer = order != null && order.getUser() != null && order.getUser().getId().equals(userId);
        boolean isSeller = seller != null && seller.getUser() != null && seller.getUser().getId().equals(userId);

        if (!isCustomer && !isSeller) {
            throw new UnauthorizedShipmentAccessException("You do not have permission to view this shipment");
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

    private String resolvePaymentMethod(OrderEntity order) {
        if (order == null) return "PREPAID";
        Optional<PaymentEntity> payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        if (payment.isPresent()) {
            return payment.get().getGateway() != null ? payment.get().getGateway() : "PREPAID";
        }
        return (order.getPaymentStatus() == PaymentStatus.PENDING) ? "COD" : "PREPAID";
    }

    private void sendMilestoneNotification(UserEntity user, String eventType, String title, String body, Long shipmentId) {
        if (user == null) return;
        try {
            fcmPushNotificationService.sendDeliveryNotification(user.getId(), eventType, title, body, shipmentId);
        } catch (Exception ex) {
            log.warn("[FCM NOTIFICATION NON-BLOCKING] Failed to send {} to user {}: {}", eventType, user.getId(), ex.getMessage());
        }
    }
}
