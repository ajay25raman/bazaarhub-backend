package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.delivery.application.dto.request.*;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryAssignmentResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.application.service.DeliveryAssignmentServiceImpl;
import com.bazaarhub.backend.delivery.application.service.DeliveryOtpService;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.exception.UnauthorizedDeliveryAccessException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryFailureReason;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.delivery.infrastructure.adapter.FcmPushNotificationService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceUnitTest {

    @Mock
    private SpringDataDeliveryAssignmentRepository assignmentRepository;

    @Mock
    private SpringDataDeliveryBoyRepository deliveryBoyRepository;

    @Mock
    private SpringDataShipmentRepository shipmentRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataPaymentRepository paymentRepository;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @Mock
    private DeliveryOtpService deliveryOtpService;

    @Mock
    private FcmPushNotificationService fcmPushNotificationService;

    @Mock
    private CloudinaryService cloudinaryService;

    @Spy
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    @InjectMocks
    private DeliveryAssignmentServiceImpl assignmentService;

    private UserEntity deliveryUser;
    private UserEntity customerUser;
    private DeliveryBoyEntity deliveryBoy;
    private OrderEntity parentOrder;
    private SellerOrderEntity sellerOrder;
    private ShipmentEntity shipment;
    private DeliveryAssignmentEntity assignment;

    @BeforeEach
    void setUp() {
        deliveryUser = UserEntity.builder().id(20L).fullName("Suresh Rider").phone("+919876543210").build();
        customerUser = UserEntity.builder().id(30L).fullName("Pooja Customer").phone("+919123456789").build();

        deliveryBoy = DeliveryBoyEntity.builder()
                .id(5L)
                .user(deliveryUser)
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH-04-XY-9999")
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build();

        parentOrder = OrderEntity.builder()
                .id(100L)
                .orderNumber("ORD-100")
                .user(customerUser)
                .recipientName("Pooja Customer")
                .recipientPhone("+919123456789")
                .addressLine1("Flat 402, Sunshine Apts")
                .city("Pune")
                .state("Maharashtra")
                .postalCode("411001")
                .country("India")
                .status(OrderStatus.PROCESSING)
                .paymentStatus(com.bazaarhub.backend.order.domain.model.PaymentStatus.PAID)
                .grandTotal(new BigDecimal("2999.00"))
                .build();

        SellerEntity seller = SellerEntity.builder().id(2L).businessName("ElectroZone").build();

        sellerOrder = SellerOrderEntity.builder()
                .id(50L)
                .order(parentOrder)
                .seller(seller)
                .subOrderNumber("SO-100-01")
                .status(SellerOrderStatus.PROCESSING)
                .totalAmount(new BigDecimal("2999.00"))
                .items(new ArrayList<>())
                .build();

        shipment = ShipmentEntity.builder()
                .id(200L)
                .sellerOrder(sellerOrder)
                .shipmentNumber("SHP-TEST-1234")
                .status(ShipmentStatus.READY_FOR_PICKUP)
                .carrierName("BazaarHub Fleet")
                .trackingNumber("TRK-TEST-1234")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assignment = DeliveryAssignmentEntity.builder()
                .id(1L)
                .shipment(shipment)
                .deliveryBoy(deliveryBoy)
                .assignmentStatus(DeliveryAssignmentStatus.ASSIGNED)
                .assignedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Admin assigns delivery partner to shipment successfully")
    void testAdminAssignDeliveryBoy_Success() {
        UserEntity admin = UserEntity.builder().id(1L).fullName("Admin").build();
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(deliveryBoyRepository.findById(5L)).thenReturn(Optional.of(deliveryBoy));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> {
            DeliveryAssignmentEntity saved = i.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        AssignDeliveryBoyRequest request = AssignDeliveryBoyRequest.builder()
                .shipmentId(200L)
                .deliveryBoyId(5L)
                .build();

        DeliveryAssignmentResponse response = assignmentService.adminAssignDeliveryBoy(1L, request);

        assertNotNull(response);
        assertEquals(DeliveryAssignmentStatus.ASSIGNED, response.getAssignmentStatus());
        verify(fcmPushNotificationService, times(1)).sendDeliveryNotification(
                eq(20L), eq("DELIVERY_ASSIGNED"), any(), any(), eq(200L));
    }

    @Test
    @DisplayName("Admin assigning unverified partner throws InvalidDeliveryOperationException")
    void testAdminAssignDeliveryBoy_Unverified_ThrowsException() {
        deliveryBoy.setVerificationStatus(DeliveryBoyVerificationStatus.PENDING);
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(deliveryBoyRepository.findById(5L)).thenReturn(Optional.of(deliveryBoy));

        AssignDeliveryBoyRequest request = AssignDeliveryBoyRequest.builder()
                .shipmentId(200L)
                .deliveryBoyId(5L)
                .build();

        assertThrows(InvalidDeliveryOperationException.class,
                () -> assignmentService.adminAssignDeliveryBoy(1L, request));
    }

    @Test
    @DisplayName("Delivery partner accepts assigned task successfully")
    void testAcceptAssignment_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));

        DeliveryAssignmentResponse response = assignmentService.acceptAssignment(20L, 1L);

        assertNotNull(response);
        assertEquals(DeliveryAssignmentStatus.ACCEPTED, response.getAssignmentStatus());
        assertNotNull(response.getAcceptedAt());
    }

    @Test
    @DisplayName("Delivery partner rejects assigned task with reason")
    void testRejectAssignment_Success() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));

        RejectAssignmentRequest request = RejectAssignmentRequest.builder()
                .reason("Heavy rainfall in destination zone")
                .build();

        DeliveryAssignmentResponse response = assignmentService.rejectAssignment(20L, 1L, request);

        assertNotNull(response);
        assertEquals(DeliveryAssignmentStatus.REJECTED, response.getAssignmentStatus());
        assertEquals("Heavy rainfall in destination zone", response.getRejectionReason());
    }

    @Test
    @DisplayName("Delivery partner confirms pickup from store, transitioning shipment to PICKED_UP and order to SHIPPED")
    void testConfirmPickup_Success() {
        assignment.setAssignmentStatus(DeliveryAssignmentStatus.ACCEPTED);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));

        PickupConfirmationRequest request = PickupConfirmationRequest.builder()
                .notes("Packaging intact and verified")
                .build();

        DeliveryAssignmentResponse response = assignmentService.confirmPickup(20L, 1L, request);

        assertNotNull(response);
        assertEquals(DeliveryAssignmentStatus.PICKED_UP, response.getAssignmentStatus());
        assertEquals(ShipmentStatus.PICKED_UP, shipment.getStatus());

        verify(orderStatusTransitionService, times(1)).transitionSellerOrderStatus(
                eq(sellerOrder), eq(SellerOrderStatus.SHIPPED), any(), eq(deliveryUser));
    }

    @Test
    @DisplayName("Marking out for delivery generates OTP and sends notification to customer")
    void testMarkOutForDelivery_Success() {
        assignment.setAssignmentStatus(DeliveryAssignmentStatus.PICKED_UP);
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(deliveryOtpService.generateAndStoreOtp(200L)).thenReturn("584920");

        DeliveryAssignmentResponse response = assignmentService.markOutForDelivery(20L, 1L);

        assertNotNull(response);
        assertEquals(ShipmentStatus.OUT_FOR_DELIVERY, shipment.getStatus());
        verify(deliveryOtpService, times(1)).generateAndStoreOtp(200L);
        verify(fcmPushNotificationService, times(1)).sendDeliveryNotification(
                eq(30L), eq("OUT_FOR_DELIVERY"), any(), any(), eq(200L));
    }

    @Test
    @DisplayName("Completing prepaid delivery verifies OTP, marks DELIVERED, and updates order")
    void testCompleteDelivery_Success_Prepaid() {
        assignment.setAssignmentStatus(DeliveryAssignmentStatus.PICKED_UP);
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        sellerOrder.setStatus(SellerOrderStatus.SHIPPED);
        parentOrder.setStatus(OrderStatus.SHIPPED);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(sellerOrderRepository.findByOrderId(100L)).thenReturn(Collections.singletonList(sellerOrder));

        CompleteDeliveryRequest request = CompleteDeliveryRequest.builder()
                .otp("584920")
                .notes("Handed directly to recipient")
                .build();

        DeliveryAssignmentResponse response = assignmentService.completeDelivery(20L, 1L, request);

        assertNotNull(response);
        assertEquals(DeliveryAssignmentStatus.COMPLETED, response.getAssignmentStatus());
        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());

        verify(deliveryOtpService, times(1)).verifyOtp(200L, "584920");
        verify(orderStatusTransitionService, times(1)).transitionSellerOrderStatus(
                eq(sellerOrder), eq(SellerOrderStatus.DELIVERED), any(), eq(deliveryUser));
    }

    @Test
    @DisplayName("Reporting failed delivery invalidates OTP and updates shipment to FAILED")
    void testFailDelivery_Success() {
        assignment.setAssignmentStatus(DeliveryAssignmentStatus.PICKED_UP);
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);

        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment));
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(assignmentRepository.save(any(DeliveryAssignmentEntity.class))).thenAnswer(i -> i.getArgument(0));

        FailedDeliveryRequest request = FailedDeliveryRequest.builder()
                .failureReason(DeliveryFailureReason.CUSTOMER_UNAVAILABLE)
                .notes("Customer not picking up phone call")
                .build();

        DeliveryAssignmentResponse response = assignmentService.failDelivery(20L, 1L, request);

        assertNotNull(response);
        assertEquals(ShipmentStatus.FAILED, shipment.getStatus());
        verify(deliveryOtpService, times(1)).invalidateOtp(200L);
    }

    @Test
    @DisplayName("Delivery partner cannot operate on assignment belonging to another partner")
    void testUnauthorizedAssignmentAccess_ThrowsException() {
        DeliveryBoyEntity otherBoy = DeliveryBoyEntity.builder().id(99L).user(deliveryUser).build();
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(assignment)); // assignment.deliveryBoy.id = 5L
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(otherBoy));

        assertThrows(UnauthorizedDeliveryAccessException.class,
                () -> assignmentService.acceptAssignment(20L, 1L));
    }
}
