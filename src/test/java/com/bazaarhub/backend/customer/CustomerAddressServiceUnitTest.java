package com.bazaarhub.backend.customer;

import com.bazaarhub.backend.customer.application.dto.request.AddressCreateRequest;
import com.bazaarhub.backend.customer.application.dto.request.AddressUpdateRequest;
import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.customer.application.service.CustomerAddressService;
import com.bazaarhub.backend.customer.domain.exception.AddressInUseException;
import com.bazaarhub.backend.customer.domain.exception.AddressNotFoundException;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.domain.model.AccountStatus;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceUnitTest {

    @Mock
    private SpringDataAddressRepository addressRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    private CustomerAddressService addressService;

    private UserEntity mockUser;
    private UserEntity otherUser;
    private AddressEntity mockAddress1;
    private AddressEntity mockAddress2;

    @BeforeEach
    void setUp() {
        addressService = new CustomerAddressService(addressRepository, userRepository);

        mockUser = UserEntity.builder()
                .id(1L)
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        otherUser = UserEntity.builder()
                .id(2L)
                .fullName("Bob Merchant")
                .email("bob@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        mockAddress1 = AddressEntity.builder()
                .id(101L)
                .user(mockUser)
                .addressType("HOME")
                .recipientName("Alice Johnson")
                .phone("+919876543210")
                .addressLine1("123 Main St")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("India")
                .isDefault(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        mockAddress2 = AddressEntity.builder()
                .id(102L)
                .user(mockUser)
                .addressType("WORK")
                .recipientName("Alice Office")
                .phone("+919876543210")
                .addressLine1("456 Tech Park")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560100")
                .country("India")
                .isDefault(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("First address automatically becomes the default address")
    void testAddFirstAddressBecomesDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.countByUserId(1L)).thenReturn(0L);
        when(addressRepository.save(any(AddressEntity.class))).thenAnswer(i -> {
            AddressEntity e = i.getArgument(0);
            e.setId(101L);
            return e;
        });

        AddressCreateRequest request = AddressCreateRequest.builder()
                .recipientName("Alice Johnson")
                .phone("+919876543210")
                .addressLine1("123 Main St")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .isDefault(false) // Even if requested false, first address becomes default
                .build();

        AddressResponse response = addressService.addAddress(1L, request);

        assertNotNull(response);
        assertTrue(response.isDefault());
        verify(addressRepository).clearDefaultAddress(1L);
    }

    @Test
    @DisplayName("Adding new address with isDefault=true clears previous default")
    void testAddAddressWithExplicitDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.countByUserId(1L)).thenReturn(1L);
        when(addressRepository.save(any(AddressEntity.class))).thenAnswer(i -> i.getArgument(0));

        AddressCreateRequest request = AddressCreateRequest.builder()
                .recipientName("Alice Work")
                .phone("+919876543210")
                .addressLine1("Tech Park")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560100")
                .isDefault(true)
                .build();

        AddressResponse response = addressService.addAddress(1L, request);

        assertNotNull(response);
        assertTrue(response.isDefault());
        verify(addressRepository).clearDefaultAddress(1L);
    }

    @Test
    @DisplayName("Get all customer addresses returns list ordered with default first")
    void testGetCustomerAddresses() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(1L))
                .thenReturn(List.of(mockAddress1, mockAddress2));

        List<AddressResponse> list = addressService.getCustomerAddresses(1L);

        assertEquals(2, list.size());
        assertTrue(list.get(0).isDefault());
        assertFalse(list.get(1).isDefault());
    }

    @Test
    @DisplayName("Get address by ID succeeds when owned by user")
    void testGetAddressByIdSuccess() {
        when(addressRepository.findById(101L)).thenReturn(Optional.of(mockAddress1));

        AddressResponse response = addressService.getAddressById(1L, 101L);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("Alice Johnson", response.getRecipientName());
    }

    @Test
    @DisplayName("Get address by ID throws UnauthorizedAddressAccessException when accessed by another user")
    void testGetAddressByIdCrossUserForbidden() {
        when(addressRepository.findById(101L)).thenReturn(Optional.of(mockAddress1));

        assertThrows(UnauthorizedAddressAccessException.class,
                () -> addressService.getAddressById(2L, 101L));
    }

    @Test
    @DisplayName("Get address by ID throws AddressNotFoundException when not found")
    void testGetAddressByIdNotFound() {
        when(addressRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class,
                () -> addressService.getAddressById(1L, 999L));
    }

    @Test
    @DisplayName("Set default address unsets previous default and updates target address")
    void testSetDefaultAddress() {
        when(addressRepository.findById(102L)).thenReturn(Optional.of(mockAddress2));
        when(addressRepository.save(any(AddressEntity.class))).thenAnswer(i -> i.getArgument(0));

        AddressResponse response = addressService.setDefaultAddress(1L, 102L);

        assertNotNull(response);
        assertTrue(response.isDefault());
        verify(addressRepository).clearDefaultAddress(1L);
    }

    @Test
    @DisplayName("Update address modifies fields and saves entity")
    void testUpdateAddressSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.findById(101L)).thenReturn(Optional.of(mockAddress1));
        when(addressRepository.save(any(AddressEntity.class))).thenAnswer(i -> i.getArgument(0));

        AddressUpdateRequest request = AddressUpdateRequest.builder()
                .addressType("WORK")
                .recipientName("Alice Updated")
                .phone("+919111111111")
                .addressLine1("Updated Street")
                .city("Mumbai")
                .state("Maharashtra")
                .postalCode("400001")
                .country("India")
                .latitude(new BigDecimal("19.0760"))
                .longitude(new BigDecimal("72.8777"))
                .isDefault(true)
                .build();

        AddressResponse response = addressService.updateAddress(1L, 101L, request);

        assertNotNull(response);
        assertEquals("Alice Updated", response.getRecipientName());
        assertEquals("Mumbai", response.getCity());
        assertEquals("400001", response.getPostalCode());
    }

    @Test
    @DisplayName("Delete default address promotes the next remaining address to default")
    void testDeleteDefaultAddressPromotesRemaining() {
        when(addressRepository.findById(101L)).thenReturn(Optional.of(mockAddress1));
        when(addressRepository.findFirstByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(Optional.of(mockAddress2));

        addressService.deleteAddress(1L, 101L);

        verify(addressRepository).delete(mockAddress1);
        verify(addressRepository).flush();
        assertTrue(mockAddress2.isDefault());
        verify(addressRepository).save(mockAddress2);
    }

    @Test
    @DisplayName("Delete address throws AddressInUseException when foreign key constraint prevents deletion")
    void testDeleteAddressInUseThrowsException() {
        when(addressRepository.findById(101L)).thenReturn(Optional.of(mockAddress1));
        doThrow(new DataIntegrityViolationException("fk_orders_address"))
                .when(addressRepository).delete(mockAddress1);

        assertThrows(AddressInUseException.class, () -> addressService.deleteAddress(1L, 101L));
    }

    @Test
    @DisplayName("Get default address returns default address when configured")
    void testGetDefaultAddressSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(mockAddress1));

        AddressResponse response = addressService.getDefaultAddress(1L);

        assertNotNull(response);
        assertTrue(response.isDefault());
        assertEquals(101L, response.getId());
    }

    @Test
    @DisplayName("Get default address throws AddressNotFoundException when none is configured")
    void testGetDefaultAddressNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(addressRepository.findByUserIdAndIsDefaultTrue(1L)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> addressService.getDefaultAddress(1L));
    }
}
