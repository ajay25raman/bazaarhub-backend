package com.bazaarhub.backend.delivery;

import com.bazaarhub.backend.delivery.application.dto.request.AdminUpdateDeliveryBoyStatusRequest;
import com.bazaarhub.backend.delivery.application.dto.request.AdminVerifyDeliveryBoyRequest;
import com.bazaarhub.backend.delivery.application.dto.request.DeliveryBoyOnboardRequest;
import com.bazaarhub.backend.delivery.application.dto.request.UpdateAvailabilityRequest;
import com.bazaarhub.backend.delivery.application.dto.response.DeliveryBoyResponse;
import com.bazaarhub.backend.delivery.application.mapper.DeliveryMapper;
import com.bazaarhub.backend.delivery.application.service.DeliveryBoyServiceImpl;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyAlreadyExistsException;
import com.bazaarhub.backend.delivery.domain.exception.DeliveryBoyNotFoundException;
import com.bazaarhub.backend.delivery.domain.exception.InvalidDeliveryOperationException;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyAvailabilityStatus;
import com.bazaarhub.backend.delivery.domain.model.DeliveryBoyVerificationStatus;
import com.bazaarhub.backend.delivery.domain.model.VehicleType;
import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.DeliveryBoyEntity;
import com.bazaarhub.backend.delivery.infrastructure.persistence.repository.SpringDataDeliveryBoyRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryBoyServiceUnitTest {

    @Mock
    private SpringDataDeliveryBoyRepository deliveryBoyRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    @Mock
    private SpringDataRoleRepository roleRepository;

    @Spy
    private DeliveryMapper deliveryMapper = new DeliveryMapper();

    @InjectMocks
    private DeliveryBoyServiceImpl deliveryBoyService;

    private UserEntity testUser;
    private RoleEntity deliveryRole;
    private DeliveryBoyEntity testDeliveryBoy;

    @BeforeEach
    void setUp() {
        deliveryRole = RoleEntity.builder().id(3L).name("ROLE_DELIVERY").description("Delivery").build();

        testUser = UserEntity.builder()
                .id(10L)
                .fullName("Ravi Kumar")
                .email("ravi.kumar@example.com")
                .phone("+919876543210")
                .roles(new HashSet<>())
                .build();

        testDeliveryBoy = DeliveryBoyEntity.builder()
                .id(1L)
                .user(testUser)
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH-01-AB-1234")
                .verificationStatus(DeliveryBoyVerificationStatus.PENDING)
                .availabilityStatus(DeliveryBoyAvailabilityStatus.OFFLINE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Successfully onboard delivery partner and assign ROLE_DELIVERY")
    void testOnboard_Success() {
        DeliveryBoyOnboardRequest request = DeliveryBoyOnboardRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH-01-AB-1234")
                .build();

        when(deliveryBoyRepository.existsByUserId(10L)).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_DELIVERY")).thenReturn(Optional.of(deliveryRole));
        when(deliveryBoyRepository.save(any(DeliveryBoyEntity.class))).thenReturn(testDeliveryBoy);

        DeliveryBoyResponse response = deliveryBoyService.onboard(10L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ravi Kumar", response.getFullName());
        assertEquals(VehicleType.BIKE, response.getVehicleType());
        assertEquals(DeliveryBoyVerificationStatus.PENDING, response.getVerificationStatus());
        assertEquals(DeliveryBoyAvailabilityStatus.OFFLINE, response.getAvailabilityStatus());

        assertTrue(testUser.getRoles().contains(deliveryRole), "User must have ROLE_DELIVERY added");
        verify(deliveryBoyRepository, times(1)).save(any(DeliveryBoyEntity.class));
    }

    @Test
    @DisplayName("Onboarding fails if user is already registered as a delivery partner")
    void testOnboard_AlreadyExists_ThrowsException() {
        DeliveryBoyOnboardRequest request = DeliveryBoyOnboardRequest.builder()
                .vehicleType(VehicleType.BIKE)
                .vehicleNumber("MH-01-AB-1234")
                .build();

        when(deliveryBoyRepository.existsByUserId(10L)).thenReturn(true);

        assertThrows(DeliveryBoyAlreadyExistsException.class,
                () -> deliveryBoyService.onboard(10L, request));
        verify(deliveryBoyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Updating availability to AVAILABLE succeeds when KYC is verified")
    void testUpdateAvailability_Verified_Success() {
        testDeliveryBoy.setVerificationStatus(DeliveryBoyVerificationStatus.VERIFIED);
        when(deliveryBoyRepository.findByUserId(10L)).thenReturn(Optional.of(testDeliveryBoy));
        when(deliveryBoyRepository.save(any(DeliveryBoyEntity.class))).thenAnswer(i -> i.getArgument(0));

        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build();

        DeliveryBoyResponse response = deliveryBoyService.updateAvailability(10L, request);

        assertNotNull(response);
        assertEquals(DeliveryBoyAvailabilityStatus.AVAILABLE, response.getAvailabilityStatus());
    }

    @Test
    @DisplayName("Updating availability to AVAILABLE fails when KYC is pending")
    void testUpdateAvailability_PendingKyc_ThrowsException() {
        testDeliveryBoy.setVerificationStatus(DeliveryBoyVerificationStatus.PENDING);
        when(deliveryBoyRepository.findByUserId(10L)).thenReturn(Optional.of(testDeliveryBoy));

        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build();

        assertThrows(InvalidDeliveryOperationException.class,
                () -> deliveryBoyService.updateAvailability(10L, request));
    }

    @Test
    @DisplayName("Updating availability fails if delivery boy account is suspended")
    void testUpdateAvailability_Suspended_ThrowsException() {
        testDeliveryBoy.setAvailabilityStatus(DeliveryBoyAvailabilityStatus.SUSPENDED);
        when(deliveryBoyRepository.findByUserId(10L)).thenReturn(Optional.of(testDeliveryBoy));

        UpdateAvailabilityRequest request = UpdateAvailabilityRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.AVAILABLE)
                .build();

        assertThrows(InvalidDeliveryOperationException.class,
                () -> deliveryBoyService.updateAvailability(10L, request));
    }

    @Test
    @DisplayName("Admin verifies delivery partner KYC")
    void testAdminVerifyDeliveryBoy_Success() {
        UserEntity admin = UserEntity.builder().id(1L).fullName("Admin User").build();
        when(deliveryBoyRepository.findById(1L)).thenReturn(Optional.of(testDeliveryBoy));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(deliveryBoyRepository.save(any(DeliveryBoyEntity.class))).thenAnswer(i -> i.getArgument(0));

        AdminVerifyDeliveryBoyRequest request = AdminVerifyDeliveryBoyRequest.builder()
                .verificationStatus(DeliveryBoyVerificationStatus.VERIFIED)
                .remarks("All documents verified")
                .build();

        DeliveryBoyResponse response = deliveryBoyService.adminVerifyDeliveryBoy(1L, 1L, request);

        assertNotNull(response);
        assertEquals(DeliveryBoyVerificationStatus.VERIFIED, response.getVerificationStatus());
        assertNotNull(response.getVerifiedAt());
    }

    @Test
    @DisplayName("Admin suspends delivery partner account")
    void testAdminUpdateStatus_Suspend() {
        UserEntity admin = UserEntity.builder().id(1L).fullName("Admin User").build();
        when(deliveryBoyRepository.findById(1L)).thenReturn(Optional.of(testDeliveryBoy));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(deliveryBoyRepository.save(any(DeliveryBoyEntity.class))).thenAnswer(i -> i.getArgument(0));

        AdminUpdateDeliveryBoyStatusRequest request = AdminUpdateDeliveryBoyStatusRequest.builder()
                .availabilityStatus(DeliveryBoyAvailabilityStatus.SUSPENDED)
                .reason("Frequent cancellations")
                .build();

        DeliveryBoyResponse response = deliveryBoyService.adminUpdateStatus(1L, 1L, request);

        assertNotNull(response);
        assertEquals(DeliveryBoyAvailabilityStatus.SUSPENDED, response.getAvailabilityStatus());
    }
}
