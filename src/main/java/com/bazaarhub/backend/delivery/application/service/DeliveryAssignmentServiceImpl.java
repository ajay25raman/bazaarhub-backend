package com.bazaarhub.backend.delivery.application.service;

import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAssignmentResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryAssignmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.UnauthorizedDeliveryAccessException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.delivery.infrastructure.adapter.FcmPushNotificationService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.domain.model.PaymentStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionStatus;
import com.bazaarhub.backend.payment.domain.model.TransactionType;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.entity.PaymentTransactionEntity;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryAssignmentServiceImpl implements DeliveryAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAssignmentServiceImpl.class);

    private final SpringDataDeliveryAssignmentRepository assignmentRepository;
    private final SpringDataDeliveryBoyRepository deliveryBoyRepository;
    private final SpringDataShipmentRepository shipmentRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataOrderRepository orderRepository;
    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final SpringDataPaymentRepository paymentRepository;
    private final OrderStatusTransitionService orderStatusTransitionService;
    private final DeliveryOtpService deliveryOtpService;
    private final FcmPushNotificationService fcmPushNotificationService;
    private final CloudinaryService cloudinaryService;
    private final DeliveryMapper deliveryMapper;

    public DeliveryAssignmentServiceImpl(SpringDataDeliveryAssignmentRepository assignmentRepository,
                                         SpringDataDeliveryBoyRepository deliveryBoyRepository,
                                         SpringDataShipmentRepository shipmentRepository,
                                         SpringDataUserRepository userRepository,
                                         SpringDataOrderRepository orderRepository,
                                         SpringDataSellerOrderRepository sellerOrderRepository,
                                         SpringDataPaymentRepository paymentRepository,
                                         OrderStatusTransitionService orderStatusTransitionService,
                                         DeliveryOtpService deliveryOtpService,
                                         FcmPushNotificationService fcmPushNotificationService,
                                         CloudinaryService cloudinaryService,
                                         DeliveryMapper deliveryMapper) {
        this.assignmentRepository = assignmentRepository;
        this.deliveryBoyRepository = deliveryBoyRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.sellerOrderRepository = sellerOrderRepository;
        this.paymentRepository = paymentRepository;
        this.orderStatusTransitionService = orderStatusTransitionService;
        this.deliveryOtpService = deliveryOtpService;
        this.fcmPushNotificationService = fcmPushNotificationService;
        this.cloudinaryService = cloudinaryService;
        this.deliveryMapper = deliveryMapper;
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse adminAssignDeliveryBoy(Long adminId, AssignDeliveryBoyRequest request) {
        ShipmentEntity shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ShipmentNotFoundException(request.getShipmentId()));

        if (shipment.getStatus() == ShipmentStatus.DELIVERED || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            throw new InvalidDeliveryOperationException("Cannot assign delivery boy to shipment in status: " + shipment.getStatus());
        }

        DeliveryBoyEntity deliveryBoy = deliveryBoyRepository.findById(request.getDeliveryBoyId())
                .orElseThrow(() -> new DeliveryBoyNotFoundException(request.getDeliveryBoyId()));

        if (deliveryBoy.getVerificationStatus() != DeliveryBoyVerificationStatus.VERIFIED) {
            throw new InvalidDeliveryOperationException("Cannot assign shipment to unverified delivery partner");
        }

        if (deliveryBoy.getAvailabilityStatus() == DeliveryBoyAvailabilityStatus.SUSPENDED) {
            throw new InvalidDeliveryOperationException("Cannot assign shipment to suspended delivery partner");
        }

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + adminId));

        DeliveryAssignmentEntity assignment = DeliveryAssignmentEntity.builder()
                .shipment(shipment)
                .deliveryBoy(deliveryBoy)
                .assignmentStatus(DeliveryAssignmentStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);
        log.info("[DELIVERY ASSIGNED] Shipment {} assigned to delivery partner ID: {}", shipment.getShipmentNumber(), deliveryBoy.getId());

        // Notify delivery partner
        if (deliveryBoy.getUser() != null) {
            fcmPushNotificationService.sendDeliveryNotification(
                    deliveryBoy.getUser().getId(),
                    "DELIVERY_ASSIGNED",
                    "New Delivery Task Assigned",
                    "You have been assigned shipment " + shipment.getShipmentNumber(),
                    shipment.getId());
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse adminReassignDeliveryBoy(Long adminId, Long assignmentId, ReassignDeliveryRequest request) {
        DeliveryAssignmentEntity existing = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new DeliveryAssignmentNotFoundException(assignmentId));

        DeliveryBoyEntity newDeliveryBoy = deliveryBoyRepository.findById(request.getNewDeliveryBoyId())
                .orElseThrow(() -> new DeliveryBoyNotFoundException(request.getNewDeliveryBoyId()));

        if (newDeliveryBoy.getVerificationStatus() != DeliveryBoyVerificationStatus.VERIFIED) {
            throw new InvalidDeliveryOperationException("Cannot reassign to unverified delivery partner");
        }

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new UserNotFoundException("Admin not found with ID: " + adminId));

        // Mark existing assignment as cancelled / reassigned
        existing.setAssignmentStatus(DeliveryAssignmentStatus.CANCELLED);
        existing.setRejectionReason("Reassigned by Admin. Reason: " + (request.getReason() != null ? request.getReason() : "Operational adjustment"));
        existing.setUpdatedBy(admin);
        existing.setUpdatedAt(Instant.now());
        assignmentRepository.save(existing);

        // Create new assignment
        DeliveryAssignmentEntity newAssignment = DeliveryAssignmentEntity.builder()
                .shipment(existing.getShipment())
                .deliveryBoy(newDeliveryBoy)
                .assignmentStatus(DeliveryAssignmentStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .createdBy(admin)
                .updatedBy(admin)
                .build();

        DeliveryAssignmentEntity saved = assignmentRepository.save(newAssignment);
        log.info("[DELIVERY REASSIGNED] Assignment {} cancelled. Reassigned shipment {} to delivery partner ID: {}",
                assignmentId, existing.getShipment().getShipmentNumber(), newDeliveryBoy.getId());

        if (newDeliveryBoy.getUser() != null) {
            fcmPushNotificationService.sendDeliveryNotification(
                    newDeliveryBoy.getUser().getId(),
                    "DELIVERY_ASSIGNED",
                    "New Delivery Task Assigned",
                    "You have been assigned shipment " + existing.getShipment().getShipmentNumber(),
                    existing.getShipment().getId());
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryAssignmentResponse> getDeliveryBoyAssignments(
            Long userId, DeliveryAssignmentStatus status, Pageable pageable) {
        DeliveryBoyEntity deliveryBoy = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new DeliveryBoyNotFoundException("Delivery partner profile not found for user ID: " + userId));

        if (status != null) {
            return assignmentRepository.findByDeliveryBoyIdAndAssignmentStatus(deliveryBoy.getId(), status, pageable)
                    .map(deliveryMapper::toAssignmentResponse);
        }
        return assignmentRepository.findByDeliveryBoyId(deliveryBoy.getId(), pageable)
                .map(deliveryMapper::toAssignmentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryAssignmentResponse getAssignmentById(Long userId, Long assignmentId, boolean isAdmin) {
        DeliveryAssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new DeliveryAssignmentNotFoundException(assignmentId));

        if (!isAdmin) {
            DeliveryBoyEntity deliveryBoy = deliveryBoyRepository.findByUserId(userId)
                    .orElseThrow(() -> new UnauthorizedDeliveryAccessException("You are not registered as a delivery partner"));
            if (!assignment.getDeliveryBoy().getId().equals(deliveryBoy.getId())) {
                throw new UnauthorizedDeliveryAccessException("You are not authorized to view this delivery assignment");
            }
        }

        return deliveryMapper.toAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse acceptAssignment(Long userId, Long assignmentId) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.ASSIGNED) {
            throw new InvalidDeliveryOperationException("Cannot accept assignment in status: " + assignment.getAssignmentStatus());
        }

        assignment.setAssignmentStatus(DeliveryAssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(Instant.now());
        assignment.setUpdatedBy(assignment.getDeliveryBoy().getUser());
        assignment.setUpdatedAt(Instant.now());

        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);
        log.info("[DELIVERY ACCEPTED] Delivery partner ID {} accepted task: {}", assignment.getDeliveryBoy().getId(), assignmentId);

        // Notify customer
        ShipmentEntity shipment = saved.getShipment();
        if (shipment.getSellerOrder() != null && shipment.getSellerOrder().getOrder() != null) {
            UserEntity customer = shipment.getSellerOrder().getOrder().getUser();
            if (customer != null) {
                fcmPushNotificationService.sendDeliveryNotification(
                        customer.getId(),
                        "DELIVERY_ACCEPTED",
                        "Delivery Partner Assigned",
                        "Delivery partner has accepted shipment " + shipment.getShipmentNumber(),
                        shipment.getId());
            }
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse rejectAssignment(Long userId, Long assignmentId, RejectAssignmentRequest request) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.ASSIGNED) {
            throw new InvalidDeliveryOperationException("Cannot reject assignment in status: " + assignment.getAssignmentStatus());
        }

        assignment.setAssignmentStatus(DeliveryAssignmentStatus.REJECTED);
        assignment.setRejectionReason(request.getReason());
        assignment.setUpdatedBy(assignment.getDeliveryBoy().getUser());
        assignment.setUpdatedAt(Instant.now());

        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);
        log.warn("[DELIVERY REJECTED] Delivery partner ID {} rejected task: {}. Reason: {}",
                assignment.getDeliveryBoy().getId(), assignmentId, request.getReason());

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse confirmPickup(Long userId, Long assignmentId, PickupConfirmationRequest request) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.ACCEPTED) {
            throw new InvalidDeliveryOperationException("Cannot confirm pickup. Assignment must be in ACCEPTED state. Current: " + assignment.getAssignmentStatus());
        }

        UserEntity deliveryUser = assignment.getDeliveryBoy().getUser();
        Instant now = Instant.now();

        assignment.setAssignmentStatus(DeliveryAssignmentStatus.PICKED_UP);
        assignment.setUpdatedBy(deliveryUser);
        assignment.setUpdatedAt(now);

        ShipmentEntity shipment = assignment.getShipment();
        shipment.setStatus(ShipmentStatus.PICKED_UP);
        shipment.setShippedAt(now);
        shipment.setUpdatedBy(deliveryUser);
        shipment.setUpdatedAt(now);

        if (request != null) {
            StringBuilder notes = new StringBuilder();
            if (shipment.getDeliveryNotes() != null && !shipment.getDeliveryNotes().isBlank()) {
                notes.append(shipment.getDeliveryNotes()).append(" | ");
            }
            if (request.getNotes() != null && !request.getNotes().isBlank()) {
                notes.append("Pickup: ").append(request.getNotes()).append(" ");
            }
            if (request.getPickupPhotoUrl() != null && !request.getPickupPhotoUrl().isBlank()) {
                String uploadedProof = cloudinaryService.uploadImage(request.getPickupPhotoUrl(), "bazaarhub/delivery/proofs");
                notes.append("[PICKUP_PROOF: ").append(uploadedProof).append("]");
            }
            if (!notes.isEmpty()) {
                shipment.setDeliveryNotes(notes.toString().trim());
            }
        }

        shipmentRepository.save(shipment);
        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);

        // Transition SellerOrder to SHIPPED if in PROCESSING
        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        if (sellerOrder != null && sellerOrder.getStatus() == SellerOrderStatus.PROCESSING) {
            orderStatusTransitionService.transitionSellerOrderStatus(
                    sellerOrder, SellerOrderStatus.SHIPPED, "Parcel picked up by delivery partner", deliveryUser);

            // Synchronize parent Order to SHIPPED if applicable
            OrderEntity parentOrder = sellerOrder.getOrder();
            if (parentOrder != null && parentOrder.getStatus() == OrderStatus.PROCESSING) {
                orderStatusTransitionService.transitionOrderStatus(
                        parentOrder, OrderStatus.SHIPPED, "Order items in transit", deliveryUser);
            }
        }

        log.info("[DELIVERY PICKED UP] Shipment {} picked up by delivery partner ID: {}", shipment.getShipmentNumber(), assignment.getDeliveryBoy().getId());

        // Notify customer
        if (sellerOrder != null && sellerOrder.getOrder() != null) {
            UserEntity customer = sellerOrder.getOrder().getUser();
            if (customer != null) {
                fcmPushNotificationService.sendDeliveryNotification(
                        customer.getId(),
                        "DELIVERY_PICKED_UP",
                        "Package Picked Up",
                        "Your package " + shipment.getShipmentNumber() + " has been picked up from the store and is on its way!",
                        shipment.getId());
            }
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse markOutForDelivery(Long userId, Long assignmentId) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.PICKED_UP) {
            throw new InvalidDeliveryOperationException("Cannot mark out for delivery. Package must be PICKED_UP first.");
        }

        UserEntity deliveryUser = assignment.getDeliveryBoy().getUser();
        Instant now = Instant.now();

        ShipmentEntity shipment = assignment.getShipment();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        shipment.setUpdatedBy(deliveryUser);
        shipment.setUpdatedAt(now);
        shipmentRepository.save(shipment);

        assignment.setUpdatedBy(deliveryUser);
        assignment.setUpdatedAt(now);
        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);

        // Generate cryptographically secure OTP for customer delivery verification
        String plainOtp = deliveryOtpService.generateAndStoreOtp(shipment.getId());

        log.info("[OUT FOR DELIVERY] Shipment {} marked OUT_FOR_DELIVERY. OTP generated and stored securely.",
                shipment.getShipmentNumber());

        // Dispatch OTP to customer via notification
        if (shipment.getSellerOrder() != null && shipment.getSellerOrder().getOrder() != null) {
            UserEntity customer = shipment.getSellerOrder().getOrder().getUser();
            if (customer != null) {
                String message = String.format(
                        "Your package %s is out for delivery! Share OTP %s with your delivery partner upon delivery.",
                        shipment.getShipmentNumber(), plainOtp);

                fcmPushNotificationService.sendDeliveryNotification(
                        customer.getId(),
                        "OUT_FOR_DELIVERY",
                        "Out for Delivery - OTP Inside",
                        message,
                        shipment.getId());
            }
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse completeDelivery(Long userId, Long assignmentId, CompleteDeliveryRequest request) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.PICKED_UP &&
            assignment.getShipment().getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new InvalidDeliveryOperationException("Cannot complete delivery. Shipment must be in OUT_FOR_DELIVERY state.");
        }

        ShipmentEntity shipment = assignment.getShipment();

        // 1. Strict Cryptographic OTP Verification
        deliveryOtpService.verifyOtp(shipment.getId(), request.getOtp());

        UserEntity deliveryUser = assignment.getDeliveryBoy().getUser();
        Instant now = Instant.now();

        // 2. COD Cash Collection Settlement Reconciliation (if applicable)
        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        OrderEntity order = sellerOrder != null ? sellerOrder.getOrder() : null;

        if (order != null && order.getPaymentStatus() == com.bazaarhub.backend.order.domain.model.PaymentStatus.PENDING) {
            log.info("[COD COLLECTION] Processing cash on delivery collection for order: {}", order.getOrderNumber());

            BigDecimal collectedAmount = (request.getCodAmountCollected() != null)
                    ? request.getCodAmountCollected()
                    : (sellerOrder != null ? sellerOrder.getTotalAmount() : order.getGrandTotal());

            // Look up existing pending payment record
            Optional<PaymentEntity> paymentOpt = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(
                    order.getId(), PaymentStatus.PENDING);

            PaymentEntity payment;
            if (paymentOpt.isPresent()) {
                payment = paymentOpt.get();
            } else {
                payment = PaymentEntity.builder()
                        .order(order)
                        .paymentReference("COD-" + shipment.getShipmentNumber())
                        .amount(order.getGrandTotal())
                        .gateway("COD")
                        .currency("INR")
                        .status(PaymentStatus.PENDING)
                        .createdBy(deliveryUser)
                        .updatedBy(deliveryUser)
                        .build();
            }

            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setPaidAt(now);
            payment.setUpdatedBy(deliveryUser);
            payment.setPaymentMethod("CASH");

            PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                    .payment(payment)
                    .gatewayTransactionId("COD-COLLECTED-" + deliveryUser.getId() + "-" + now.getEpochSecond())
                    .transactionType(TransactionType.PAYMENT)
                    .amount(collectedAmount)
                    .status(TransactionStatus.SUCCESS)
                    .processedAt(now)
                    .createdBy(deliveryUser)
                    .updatedBy(deliveryUser)
                    .build();
            payment.addTransaction(tx);
            paymentRepository.save(payment);

            order.setPaymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("[COD SETTLED] Order {} marked as PAID upon successful delivery cash collection", order.getOrderNumber());
        }

        // 3. Update Assignment and Shipment states
        assignment.setAssignmentStatus(DeliveryAssignmentStatus.COMPLETED);
        assignment.setCompletedAt(now);
        assignment.setUpdatedBy(deliveryUser);
        assignment.setUpdatedAt(now);

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(now);
        shipment.setUpdatedBy(deliveryUser);
        shipment.setUpdatedAt(now);

        StringBuilder notes = new StringBuilder();
        if (shipment.getDeliveryNotes() != null && !shipment.getDeliveryNotes().isBlank()) {
            notes.append(shipment.getDeliveryNotes()).append(" | ");
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            notes.append("Delivery: ").append(request.getNotes()).append(" ");
        }
        if (request.getDeliveryProofUrl() != null && !request.getDeliveryProofUrl().isBlank()) {
            String uploadedProof = cloudinaryService.uploadImage(request.getDeliveryProofUrl(), "bazaarhub/delivery/proofs");
            notes.append("[DELIVERY_PROOF: ").append(uploadedProof).append("]");
        }
        if (!notes.isEmpty()) {
            shipment.setDeliveryNotes(notes.toString().trim());
        }

        shipmentRepository.save(shipment);
        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);

        // 4. Transition Order and SellerOrder states to DELIVERED
        if (sellerOrder != null && sellerOrder.getStatus() == SellerOrderStatus.SHIPPED) {
            orderStatusTransitionService.transitionSellerOrderStatus(
                    sellerOrder, SellerOrderStatus.DELIVERED, "Delivered by partner ID " + assignment.getDeliveryBoy().getId(), deliveryUser);

            if (order != null) {
                List<SellerOrderEntity> allSellerOrders = sellerOrderRepository.findByOrderId(order.getId());
                boolean allDelivered = allSellerOrders.stream().allMatch(so -> so.getStatus() == SellerOrderStatus.DELIVERED);
                if (allDelivered && order.getStatus() == OrderStatus.SHIPPED) {
                    orderStatusTransitionService.transitionOrderStatus(
                            order, OrderStatus.DELIVERED, "All package shipments delivered successfully", deliveryUser);
                }
            }
        }

        log.info("[DELIVERY COMPLETED] Shipment {} successfully delivered by partner ID: {}",
                shipment.getShipmentNumber(), assignment.getDeliveryBoy().getId());

        // 5. Notify customer
        if (order != null && order.getUser() != null) {
            fcmPushNotificationService.sendDeliveryNotification(
                    order.getUser().getId(),
                    "DELIVERY_COMPLETED",
                    "Package Delivered!",
                    "Your package " + shipment.getShipmentNumber() + " has been successfully delivered.",
                    shipment.getId());
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryAssignmentResponse failDelivery(Long userId, Long assignmentId, FailedDeliveryRequest request) {
        DeliveryAssignmentEntity assignment = getOwnedAssignment(userId, assignmentId);

        if (assignment.getAssignmentStatus() != DeliveryAssignmentStatus.PICKED_UP &&
            assignment.getShipment().getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new InvalidDeliveryOperationException("Cannot fail delivery. Package must be in transit or out for delivery.");
        }

        UserEntity deliveryUser = assignment.getDeliveryBoy().getUser();
        Instant now = Instant.now();
        ShipmentEntity shipment = assignment.getShipment();

        // Invalidate OTP
        deliveryOtpService.invalidateOtp(shipment.getId());

        shipment.setStatus(ShipmentStatus.FAILED);
        shipment.setUpdatedBy(deliveryUser);
        shipment.setUpdatedAt(now);

        StringBuilder notes = new StringBuilder();
        if (shipment.getDeliveryNotes() != null && !shipment.getDeliveryNotes().isBlank()) {
            notes.append(shipment.getDeliveryNotes()).append(" | ");
        }
        notes.append("FAILED [Reason: ").append(request.getFailureReason()).append("]");
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            notes.append(" Note: ").append(request.getNotes()).append(" ");
        }
        if (request.getEvidencePhotoUrl() != null && !request.getEvidencePhotoUrl().isBlank()) {
            String uploadedEvidence = cloudinaryService.uploadImage(request.getEvidencePhotoUrl(), "bazaarhub/delivery/failures");
            notes.append("[EVIDENCE: ").append(uploadedEvidence).append("]");
        }
        shipment.setDeliveryNotes(notes.toString().trim());

        shipmentRepository.save(shipment);

        assignment.setUpdatedBy(deliveryUser);
        assignment.setUpdatedAt(now);
        DeliveryAssignmentEntity saved = assignmentRepository.save(assignment);

        log.warn("[DELIVERY FAILED] Shipment {} marked FAILED by delivery partner ID {}. Reason: {}",
                shipment.getShipmentNumber(), assignment.getDeliveryBoy().getId(), request.getFailureReason());

        // Notify customer
        SellerOrderEntity sellerOrder = shipment.getSellerOrder();
        if (sellerOrder != null && sellerOrder.getOrder() != null && sellerOrder.getOrder().getUser() != null) {
            fcmPushNotificationService.sendDeliveryNotification(
                    sellerOrder.getOrder().getUser().getId(),
                    "DELIVERY_FAILED",
                    "Delivery Attempt Failed",
                    "Delivery attempt failed for shipment " + shipment.getShipmentNumber() + ". Reason: " + request.getFailureReason(),
                    shipment.getId());
        }

        return deliveryMapper.toAssignmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryAssignmentResponse> adminListAssignments(
            Long deliveryBoyId, Long shipmentId, DeliveryAssignmentStatus status, Pageable pageable) {
        return assignmentRepository.searchAssignments(deliveryBoyId, shipmentId, status, pageable)
                .map(deliveryMapper::toAssignmentResponse);
    }

    private DeliveryAssignmentEntity getOwnedAssignment(Long userId, Long assignmentId) {
        DeliveryAssignmentEntity assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new DeliveryAssignmentNotFoundException(assignmentId));

        DeliveryBoyEntity deliveryBoy = deliveryBoyRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedDeliveryAccessException("You are not registered as a delivery partner"));

        if (!assignment.getDeliveryBoy().getId().equals(deliveryBoy.getId())) {
            throw new UnauthorizedDeliveryAccessException("You are not authorized to perform operations on this delivery assignment");
        }

        return assignment;
    }
}
