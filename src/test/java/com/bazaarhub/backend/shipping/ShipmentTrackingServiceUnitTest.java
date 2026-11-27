package com.bazaarhub.backend.shipping;

import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.domain.exception.ShipmentNotFoundException;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryAssignmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryTrackingRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.shipping.application.dto.response.PublicTrackingResponse;
import com.bazaarhub.backend.shipping.application.dto.response.TrackingHistoryResponse;
import com.bazaarhub.backend.shipping.application.mapper.ShippingMapper;
import com.bazaarhub.backend.shipping.application.service.ShipmentTrackingService;
import com.bazaarhub.backend.shipping.application.service.ShipmentTrackingServiceImpl;
import com.bazaarhub.backend.shipping.domain.exception.UnauthorizedShipmentAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentTrackingServiceUnitTest {

    @Mock
    private SpringDataShipmentRepository shipmentRepository;

    @Mock
    private SpringDataDeliveryAssignmentRepository assignmentRepository;

    @Mock
    private SpringDataDeliveryTrackingRepository trackingRepository;

    private ShippingMapper shippingMapper;
    private DeliveryMapper deliveryMapper;
    private ShipmentTrackingService trackingService;

    private ShipmentEntity shipment;
    private UserEntity customer;

    @BeforeEach
    void setUp() {
        shippingMapper = new ShippingMapper();
        deliveryMapper = new DeliveryMapper();
        trackingService = new ShipmentTrackingServiceImpl(
                shipmentRepository,
                assignmentRepository,
                trackingRepository,
                shippingMapper,
                deliveryMapper
        );

        customer = UserEntity.builder().id(50L).fullName("Alice Customer").build();

        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .user(customer)
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .recipientName("Alice Customer")
                .recipientPhone("+91 9988776655")
                .addressLine1("100 Indiranagar")
                .build();

        SellerEntity seller = SellerEntity.builder().id(10L).businessName("TechStore").build();

        SellerOrderEntity sellerOrder = SellerOrderEntity.builder()
                .id(200L)
                .order(order)
                .seller(seller)
                .subOrderNumber("SO-100-1")
                .build();

        shipment = ShipmentEntity.builder()
                .id(300L)
                .sellerOrder(sellerOrder)
                .shipmentNumber("SHP-20261114-112233")
                .trackingNumber("BZH-20261114-112233")
                .carrierName("BazaarHub Express")
                .status(ShipmentStatus.IN_TRANSIT)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Public tracking lookup by tracking number returns masked public details")
    void testTrackByTrackingNumberSuccess() {
        when(shipmentRepository.findByTrackingNumber("BZH-20261114-112233")).thenReturn(Optional.of(shipment));

        PublicTrackingResponse response = trackingService.trackByTrackingNumber("BZH-20261114-112233");

        assertNotNull(response);
        assertEquals("BZH-20261114-112233", response.getTrackingNumber());
        assertEquals("SHP-20261114-112233", response.getShipmentNumber());
        assertEquals("Bengaluru", response.getDestinationCity());
        assertEquals(ShipmentStatus.IN_TRANSIT, response.getStatus());
        assertNotNull(response.getMilestones());
        assertFalse(response.getMilestones().isEmpty());
    }

    @Test
    @DisplayName("Public tracking lookup throws exception when tracking number is not found")
    void testTrackByTrackingNumberNotFound() {
        when(shipmentRepository.findByTrackingNumber("NON_EXISTENT")).thenReturn(Optional.empty());
        when(shipmentRepository.findByShipmentNumber("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class, () ->
                trackingService.trackByTrackingNumber("NON_EXISTENT"));
    }

    @Test
    @DisplayName("Authenticated tracking history returns full delivery partner and breadcrumb details")
    void testGetTrackingHistorySuccess() {
        UserEntity boyUser = UserEntity.builder()
                .id(77L)
                .fullName("Rajesh Delivery")
                .phone("+919876543210")
                .build();

        DeliveryBoyEntity deliveryBoy = DeliveryBoyEntity.builder()
                .id(7L)
                .user(boyUser)
                .vehicleType(VehicleType.BIKE)
                .build();

        DeliveryAssignmentEntity assignment = DeliveryAssignmentEntity.builder()
                .id(1L)
                .shipment(shipment)
                .deliveryBoy(deliveryBoy)
                .build();

        when(shipmentRepository.findById(300L)).thenReturn(Optional.of(shipment));
        when(assignmentRepository.findFirstByShipmentIdAndAssignmentStatusNotOrderByAssignedAtDesc(eq(300L), any()))
                .thenReturn(Optional.of(assignment));
        when(trackingRepository.findByShipmentIdOrderByRecordedAtDesc(eq(300L), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        TrackingHistoryResponse response = trackingService.getTrackingHistory(50L, 300L, false);

        assertNotNull(response);
        assertEquals("SHP-20261114-112233", response.getShipmentNumber());
        assertEquals("Rajesh Delivery", response.getDeliveryPartnerName());
        assertEquals("BIKE", response.getVehicleType());
        assertTrue(response.getDeliveryPartnerPhone().contains("*****")); // Masked
        assertNotNull(response.getMilestones());
    }

    @Test
    @DisplayName("Authenticated tracking history rejects unauthorized user")
    void testGetTrackingHistoryUnauthorized() {
        when(shipmentRepository.findById(300L)).thenReturn(Optional.of(shipment));

        assertThrows(UnauthorizedShipmentAccessException.class, () ->
                trackingService.getTrackingHistory(999L, 300L, false));
    }
}
