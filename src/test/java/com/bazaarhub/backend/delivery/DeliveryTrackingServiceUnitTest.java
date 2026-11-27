package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.delivery.application.dto.request.UpdateLocationRequest;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryTrackingResponse;
import com.bazaarhub.backend.delivery.application.dto.response.RouteEstimateResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.application.service.DeliveryTrackingServiceImpl;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.exception.UnauthorizedDeliveryAccessException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryAssignmentStatus;
import com.bazaarhub.backend.delivery.domain.model.ShipmentStatus;
import com.bazaarhub.backend.delivery.infrastructure.adapter.GoogleMapsService;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryTrackingEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryAssignmentRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryTrackingRepository;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataShipmentRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryTrackingServiceUnitTest {

    @Mock
    private SpringDataDeliveryTrackingRepository trackingRepository;

    @Mock
    private SpringDataDeliveryBoyRepository deliveryBoyRepository;

    @Mock
    private SpringDataShipmentRepository shipmentRepository;

    @Mock
    private SpringDataDeliveryAssignmentRepository assignmentRepository;

    @Mock
    private GoogleMapsService googleMapsService;

    @Spy
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    @InjectMocks
    private DeliveryTrackingServiceImpl trackingService;

    private DeliveryBoyEntity deliveryBoy;
    private ShipmentEntity shipment;
    private UserEntity customerUser;

    @BeforeEach
    void setUp() {
        customerUser = UserEntity.builder().id(10L).fullName("Customer").build();
        UserEntity riderUser = UserEntity.builder().id(20L).fullName("Rider").build();
        deliveryBoy = DeliveryBoyEntity.builder().id(5L).user(riderUser).build();

        OrderEntity order = OrderEntity.builder()
                .id(100L)
                .orderNumber("ORD-100")
                .user(customerUser)
                .addressLine1("Linking Road")
                .city("Mumbai")
                .postalCode("400050")
                .build();

        SellerOrderEntity sellerOrder = SellerOrderEntity.builder().id(50L).order(order).build();

        shipment = ShipmentEntity.builder()
                .id(200L)
                .shipmentNumber("SHP-200")
                .sellerOrder(sellerOrder)
                .status(ShipmentStatus.OUT_FOR_DELIVERY)
                .build();
    }

    @Test
    @DisplayName("Successfully records real-time GPS coordinate for active shipment")
    void testRecordLocation_Success() {
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(trackingRepository.save(any(DeliveryTrackingEntity.class))).thenAnswer(i -> {
            DeliveryTrackingEntity entity = i.getArgument(0);
            entity.setId(1001L);
            return entity;
        });

        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .shipmentId(200L)
                .latitude(new BigDecimal("19.076090"))
                .longitude(new BigDecimal("72.877426"))
                .build();

        DeliveryTrackingResponse response = trackingService.recordLocation(20L, request);

        assertNotNull(response);
        assertEquals(1001L, response.getId());
        assertEquals(new BigDecimal("19.076090"), response.getLatitude());
        assertEquals(new BigDecimal("72.877426"), response.getLongitude());
        verify(trackingRepository, times(1)).save(any(DeliveryTrackingEntity.class));
    }

    @Test
    @DisplayName("Fails to record location if shipment is not in active transit state")
    void testRecordLocation_InvalidShipmentState_ThrowsException() {
        shipment.setStatus(ShipmentStatus.READY_FOR_PICKUP);
        when(deliveryBoyRepository.findByUserId(20L)).thenReturn(Optional.of(deliveryBoy));
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));

        UpdateLocationRequest request = UpdateLocationRequest.builder()
                .shipmentId(200L)
                .latitude(new BigDecimal("19.076090"))
                .longitude(new BigDecimal("72.877426"))
                .build();

        assertThrows(InvalidDeliveryOperationException.class,
                () -> trackingService.recordLocation(20L, request));
    }

    @Test
    @DisplayName("Calculates route distance and duration estimate using GoogleMapsService")
    void testGetRouteEstimate_Success() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(200L)).thenReturn(Optional.of(
                DeliveryTrackingEntity.builder()
                        .latitude(new BigDecimal("19.070000"))
                        .longitude(new BigDecimal("72.870000"))
                        .build()
        ));

        when(googleMapsService.calculateRoute(any(), any(), any(), any(), any(), any()))
                .thenReturn(new GoogleMapsService.RouteDetails(new BigDecimal("4.85"), 18, "Google Maps Transit"));

        RouteEstimateResponse response = trackingService.getRouteEstimate(10L, 200L, false);

        assertNotNull(response);
        assertEquals(new BigDecimal("4.85"), response.getDistanceKm());
        assertEquals(18, response.getDurationMinutes());
        assertEquals("Google Maps Transit", response.getSummary());
    }

    @Test
    @DisplayName("Shipment owner customer can access latest live location")
    void testGetLatestLocation_CustomerOwner_Success() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(200L)).thenReturn(Optional.of(
                DeliveryTrackingEntity.builder()
                        .id(101L)
                        .shipment(shipment)
                        .deliveryBoy(deliveryBoy)
                        .latitude(new BigDecimal("19.076090"))
                        .longitude(new BigDecimal("72.877426"))
                        .recordedAt(Instant.now())
                        .build()
        ));

        DeliveryTrackingResponse response = trackingService.getLatestLocation(10L, 200L, false);

        assertNotNull(response);
        assertEquals(new BigDecimal("19.076090"), response.getLatitude());
        assertEquals(new BigDecimal("72.877426"), response.getLongitude());
    }

    @Test
    @DisplayName("Different customer is rejected with UnauthorizedDeliveryAccessException")
    void testGetLatestLocation_DifferentCustomer_ThrowsUnauthorizedDeliveryAccessException() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(assignmentRepository.isDeliveryBoyAssignedToShipment(eq(200L), eq(999L), any())).thenReturn(false);

        assertThrows(UnauthorizedDeliveryAccessException.class,
                () -> trackingService.getLatestLocation(999L, 200L, false));
    }

    @Test
    @DisplayName("Assigned delivery partner can access live location")
    void testGetLatestLocation_AssignedRider_Success() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(assignmentRepository.isDeliveryBoyAssignedToShipment(eq(200L), eq(20L), any())).thenReturn(true);
        when(trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(200L)).thenReturn(Optional.of(
                DeliveryTrackingEntity.builder()
                        .id(101L)
                        .shipment(shipment)
                        .deliveryBoy(deliveryBoy)
                        .latitude(new BigDecimal("19.076090"))
                        .longitude(new BigDecimal("72.877426"))
                        .recordedAt(Instant.now())
                        .build()
        ));

        DeliveryTrackingResponse response = trackingService.getLatestLocation(20L, 200L, false);

        assertNotNull(response);
        assertEquals(new BigDecimal("19.076090"), response.getLatitude());
    }

    @Test
    @DisplayName("Unassigned delivery partner is rejected with UnauthorizedDeliveryAccessException")
    void testGetLatestLocation_UnassignedRider_ThrowsUnauthorizedDeliveryAccessException() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(assignmentRepository.isDeliveryBoyAssignedToShipment(eq(200L), eq(33L), any())).thenReturn(false);

        assertThrows(UnauthorizedDeliveryAccessException.class,
                () -> trackingService.getLatestLocation(33L, 200L, false));
    }

    @Test
    @DisplayName("Admin can access live location regardless of order ownership")
    void testGetLatestLocation_Admin_Success() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(trackingRepository.findFirstByShipmentIdOrderByRecordedAtDesc(200L)).thenReturn(Optional.of(
                DeliveryTrackingEntity.builder()
                        .id(101L)
                        .shipment(shipment)
                        .deliveryBoy(deliveryBoy)
                        .latitude(new BigDecimal("19.076090"))
                        .longitude(new BigDecimal("72.877426"))
                        .recordedAt(Instant.now())
                        .build()
        ));

        DeliveryTrackingResponse response = trackingService.getLatestLocation(999L, 200L, true);

        assertNotNull(response);
        assertEquals(new BigDecimal("19.076090"), response.getLatitude());
    }

    @Test
    @DisplayName("Unassigned rider cannot get route estimate")
    void testGetRouteEstimate_UnassignedRider_ThrowsUnauthorizedDeliveryAccessException() {
        when(shipmentRepository.findById(200L)).thenReturn(Optional.of(shipment));
        when(assignmentRepository.isDeliveryBoyAssignedToShipment(eq(200L), eq(33L), any())).thenReturn(false);

        assertThrows(UnauthorizedDeliveryAccessException.class,
                () -> trackingService.getRouteEstimate(33L, 200L, false));
    }
}
