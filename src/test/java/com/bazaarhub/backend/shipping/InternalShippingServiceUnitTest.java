package com.bazaarhub.backend.shipping;

import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.CloudinaryService;
import com.bazaarhub.backend.delivery.infrastructure.adapter.FcmPushNotificationService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.order.application.service.OrderStatusTransitionService;
import com.bazaarhub.backend.order.domain.model.OrderStatus;
import com.bazaarhub.backend.order.domain.model.PaymentStatus;
import com.bazaarhub.backend.order.domain.model.SellerOrderStatus;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataOrderRepository;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shipping.application.dto.request.CancelShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.application.dto.request.ReadyForPickupRequest;
import com.bazaarhub.backend.shipping.application.dto.response.InternalShipmentResponse;
import com.bazaarhub.backend.shipping.application.mapper.ShippingMapper;
import com.bazaarhub.backend.shipping.application.service.InternalShippingService;
import com.bazaarhub.backend.shipping.application.service.InternalShippingServiceImpl;
import com.bazaarhub.backend.shipping.domain.exception.IneligibleOrderForShipmentException;
import com.bazaarhub.backend.shipping.domain.exception.InvalidShipmentOperationException;
import com.bazaarhub.backend.shipping.domain.exception.ShipmentAlreadyExistsException;
import com.bazaarhub.backend.shipping.domain.exception.UnauthorizedShipmentAccessException;
import com.bazaarhub.backend.shipping.infrastructure.provider.InternalShippingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalShippingServiceUnitTest {

    @Mock
    private SpringDataShipmentRepository shipmentRepository;

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private SpringDataOrderRepository orderRepository;

    @Mock
    private SpringDataSellerRepository sellerRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataPaymentRepository paymentRepository;

    @Mock
    private SpringDataDeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private InternalShippingProvider internalShippingProvider;

    @Mock
    private OrderStatusTransitionService orderStatusTransitionService;

    @Mock
    private FcmPushNotificationService fcmPushNotificationService;

    @Mock
    private CloudinaryService cloudinaryService;

    private ShippingMapper shippingMapper;
    private InternalShippingService shippingService;

    private UserEntity sellerUser;
    private SellerEntity seller;
    private OrderEntity order;
    private SellerOrderEntity sellerOrder;
    private ShipmentEntity shipment;

    @BeforeEach
    void setUp() {
        shippingMapper = new ShippingMapper();
        shippingService = new InternalShippingServiceImpl(
                shipmentRepository,
                sellerOrderRepository,
                orderRepository,
                sellerRepository,
                userRepository,
                paymentRepository,
                deliveryAssignmentRepository,
                internalShippingProvider,
                orderStatusTransitionService,
                fcmPushNotificationService,
                cloudinaryService,
                shippingMapper
        );

        sellerUser = UserEntity.builder()
                .id(10L)
                .email("seller@bazaarhub.com")
                .fullName("Seller One")
                .build();

        seller = SellerEntity.builder()
                .id(1L)
                .user(sellerUser)
                .businessName("Apex Store")
                .approvalStatus(SellerApprovalStatus.APPROVED)
                .build();

        UserEntity customerUser = UserEntity.builder()
                .id(20L)
                .email("customer@bazaarhub.com")
                .fullName("Customer One")
                .build();

        order = OrderEntity.builder()
                .id(100L)
                .orderNumber("ORD-100")
                .user(customerUser)
                .status(OrderStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .recipientName("Customer One")
                .recipientPhone("+91 9876543210")
                .addressLine1("123 MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .grandTotal(new BigDecimal("999.00"))
                .build();

        sellerOrder = SellerOrderEntity.builder()
                .id(200L)
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-100-1")
                .status(SellerOrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("900.00"))
                .deliveryFee(new BigDecimal("40.00"))
                .totalAmount(new BigDecimal("940.00"))
                .build();

        shipment = ShipmentEntity.builder()
                .id(500L)
                .sellerOrder(sellerOrder)
                .shipmentNumber("SHP-20261114-123456")
                .trackingNumber("BZH-20261114-123456")
                .carrierName("BazaarHub Express")
                .status(ShipmentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Create shipment successfully for eligible seller sub-order")
    void testCreateShipmentSuccess() {
        CreateInternalShipmentRequest request = CreateInternalShipmentRequest.builder()
                .sellerOrderId(200L)
                .carrierName("BazaarHub Express")
                .packageWeight(new BigDecimal("1.500"))
                .build();

        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(seller));
        when(shipmentRepository.existsBySellerOrderId(200L)).thenReturn(false);
        when(internalShippingProvider.createShipment(eq(sellerOrder), eq(request), eq(sellerUser))).thenReturn(shipment);

        InternalShipmentResponse response = shippingService.createShipment(10L, request, false);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals("SHP-20261114-123456", response.getShipmentNumber());
        assertEquals("BZH-20261114-123456", response.getTrackingNumber());
        assertEquals(ShipmentStatus.PENDING, response.getStatus());
        verify(internalShippingProvider).createShipment(eq(sellerOrder), eq(request), eq(sellerUser));
    }

    @Test
    @DisplayName("Create shipment rejects duplicate if shipment already exists")
    void testCreateShipmentDuplicateRejection() {
        CreateInternalShipmentRequest request = CreateInternalShipmentRequest.builder()
                .sellerOrderId(200L)
                .build();

        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(seller));
        when(shipmentRepository.existsBySellerOrderId(200L)).thenReturn(true);

        assertThrows(ShipmentAlreadyExistsException.class, () ->
                shippingService.createShipment(10L, request, false));

        verify(internalShippingProvider, never()).createShipment(any(), any(), any());
    }

    @Test
    @DisplayName("Create shipment rejects unauthorized seller attempting to ship another merchant's order")
    void testCreateShipmentUnauthorizedSeller() {
        SellerEntity otherSeller = SellerEntity.builder().id(99L).approvalStatus(SellerApprovalStatus.APPROVED).build();
        CreateInternalShipmentRequest request = CreateInternalShipmentRequest.builder()
                .sellerOrderId(200L)
                .build();

        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(otherSeller));

        assertThrows(UnauthorizedShipmentAccessException.class, () ->
                shippingService.createShipment(10L, request, false));
    }

    @Test
    @DisplayName("Create shipment rejects order with failed payment")
    void testCreateShipmentFailedPayment() {
        order.setPaymentStatus(PaymentStatus.FAILED);
        CreateInternalShipmentRequest request = CreateInternalShipmentRequest.builder()
                .sellerOrderId(200L)
                .build();

        when(sellerOrderRepository.findById(200L)).thenReturn(Optional.of(sellerOrder));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(seller));
        when(shipmentRepository.existsBySellerOrderId(200L)).thenReturn(false);

        assertThrows(IneligibleOrderForShipmentException.class, () ->
                shippingService.createShipment(10L, request, false));
    }

    @Test
    @DisplayName("Mark ready for pickup successfully updates status and encodes package info")
    void testMarkReadyForPickupSuccess() {
        ReadyForPickupRequest request = ReadyForPickupRequest.builder()
                .packageWeight(new BigDecimal("1.800"))
                .isFragile(true)
                .notes("Packed at Counter A")
                .build();

        when(shipmentRepository.findById(500L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(seller));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        InternalShipmentResponse response = shippingService.markReadyForPickup(10L, 500L, request);

        assertNotNull(response);
        assertEquals(ShipmentStatus.READY_FOR_PICKUP, response.getStatus());
        assertTrue(response.getDeliveryNotes().contains("fragile=true"));
        verify(shipmentRepository).save(shipment);
    }

    @Test
    @DisplayName("Cancel shipment transitions status to CANCELLED and cancels active assignment")
    void testCancelShipmentSuccess() {
        CancelShipmentRequest request = CancelShipmentRequest.builder()
                .reason("Customer changed mind")
                .build();

        when(shipmentRepository.findById(500L)).thenReturn(Optional.of(shipment));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sellerUser));
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(seller));
        when(deliveryAssignmentRepository.findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(eq(500L), any()))
                .thenReturn(Optional.empty());

        ShipmentEntity cancelledShipment = ShipmentEntity.builder()
                .id(500L)
                .sellerOrder(sellerOrder)
                .shipmentNumber("SHP-20261114-123456")
                .trackingNumber("BZH-20261114-123456")
                .status(ShipmentStatus.CANCELLED)
                .deliveryNotes("[CANCELLED: Customer changed mind]")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(internalShippingProvider.cancelShipment(eq(shipment), eq("Customer changed mind"), eq(sellerUser)))
                .thenReturn(cancelledShipment);

        InternalShipmentResponse response = shippingService.cancelShipment(10L, 500L, request, false);

        assertNotNull(response);
        assertEquals(ShipmentStatus.CANCELLED, response.getStatus());
        verify(internalShippingProvider).cancelShipment(eq(shipment), eq("Customer changed mind"), eq(sellerUser));
    }
}
