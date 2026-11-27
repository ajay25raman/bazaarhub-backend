package com.bazaarhub.backend.customer;

import com.bazaarhub.backend.customer.application.dto.request.CustomerProfileCreateRequest;
import com.bazaarhub.backend.customer.application.dto.request.CustomerProfileUpdateRequest;
import com.bazaarhub.backend.customer.application.dto.response.CustomerProfileResponse;
import com.bazaarhub.backend.customer.application.service.CustomerProfileService;
import com.bazaarhub.backend.customer.domain.exception.CustomerProfileAlreadyExistsException;
import com.bazaarhub.backend.customer.domain.exception.CustomerProfileNotFoundException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.CustomerProfileEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataCustomerProfileRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceUnitTest {

    @Mock
    private SpringDataCustomerProfileRepository profileRepository;

    @Mock
    private SpringDataUserRepository userRepository;

    private CustomerProfileService profileService;

    private UserEntity mockUser;
    private CustomerProfileEntity mockProfile;

    @BeforeEach
    void setUp() {
        profileService = new CustomerProfileService(profileRepository, userRepository);

        mockUser = UserEntity.builder()
                .id(1L)
                .fullName("Alice Johnson")
                .email("alice@example.com")
                .phone("+919876543210")
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .phoneVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        mockProfile = CustomerProfileEntity.builder()
                .id(10L)
                .user(mockUser)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender("FEMALE")
                .profileImageUrl("https://images.bazaarhub.com/profiles/alice.jpg")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Get profile successfully when profile exists")
    void testGetProfileSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        CustomerProfileResponse response = profileService.getProfile(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Alice Johnson", response.getFullName());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals(LocalDate.of(1995, 5, 15), response.getDateOfBirth());
        assertEquals("FEMALE", response.getGender());
    }

    @Test
    @DisplayName("Get profile throws CustomerProfileNotFoundException when missing")
    void testGetProfileNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(CustomerProfileNotFoundException.class, () -> profileService.getProfile(1L));
    }

    @Test
    @DisplayName("Get account profile returns basic user details even if profile is not created")
    void testGetAccountProfileWithoutProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CustomerProfileResponse response = profileService.getAccountProfile(1L);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Alice Johnson", response.getFullName());
        assertNull(response.getDateOfBirth());
        assertNull(response.getGender());
    }

    @Test
    @DisplayName("Create profile successfully creates new profile entity")
    void testCreateProfileSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.existsByUserId(1L)).thenReturn(false);
        when(profileRepository.save(any(CustomerProfileEntity.class))).thenAnswer(invocation -> {
            CustomerProfileEntity e = invocation.getArgument(0);
            e.setId(20L);
            return e;
        });

        CustomerProfileCreateRequest request = CustomerProfileCreateRequest.builder()
                .fullName("Alice B. Johnson")
                .dateOfBirth(LocalDate.of(1996, 6, 20))
                .gender("FEMALE")
                .profileImageUrl("https://images.bazaarhub.com/profiles/alice2.jpg")
                .build();

        CustomerProfileResponse response = profileService.createProfile(1L, request);

        assertNotNull(response);
        assertEquals(20L, response.getId());
        assertEquals("Alice B. Johnson", response.getFullName());
        assertEquals("FEMALE", response.getGender());

        ArgumentCaptor<CustomerProfileEntity> captor = ArgumentCaptor.forClass(CustomerProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(LocalDate.of(1996, 6, 20), captor.getValue().getDateOfBirth());
    }

    @Test
    @DisplayName("Create profile throws duplicate exception when profile already exists")
    void testCreateProfileDuplicate() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.existsByUserId(1L)).thenReturn(true);

        CustomerProfileCreateRequest request = CustomerProfileCreateRequest.builder()
                .dateOfBirth(LocalDate.of(1996, 6, 20))
                .gender("FEMALE")
                .build();

        assertThrows(CustomerProfileAlreadyExistsException.class, () -> profileService.createProfile(1L, request));
        verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Update profile updates existing profile and user details")
    void testUpdateProfileSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.save(any(CustomerProfileEntity.class))).thenReturn(mockProfile);

        CustomerProfileUpdateRequest request = CustomerProfileUpdateRequest.builder()
                .fullName("Alice Modified")
                .phone("+919999999999")
                .dateOfBirth(LocalDate.of(1994, 1, 1))
                .gender("OTHER")
                .profileImageUrl("https://images.bazaarhub.com/new.jpg")
                .build();

        CustomerProfileResponse response = profileService.updateProfile(1L, request);

        assertNotNull(response);
        assertEquals("Alice Modified", mockUser.getFullName());
        assertEquals("+919999999999", mockUser.getPhone());
        assertEquals("OTHER", mockProfile.getGender());
        assertEquals(LocalDate.of(1994, 1, 1), mockProfile.getDateOfBirth());
        verify(profileRepository).save(mockProfile);
    }

    @Test
    @DisplayName("Update DOB updates date of birth")
    void testUpdateDateOfBirth() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.save(any())).thenReturn(mockProfile);

        LocalDate newDob = LocalDate.of(1998, 12, 25);
        CustomerProfileResponse response = profileService.updateDateOfBirth(1L, newDob);

        assertNotNull(response);
        assertEquals(newDob, mockProfile.getDateOfBirth());
    }

    @Test
    @DisplayName("Update gender updates gender in uppercase")
    void testUpdateGender() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        when(profileRepository.save(any())).thenReturn(mockProfile);

        CustomerProfileResponse response = profileService.updateGender(1L, "prefer_not_to_say");

        assertNotNull(response);
        assertEquals("PREFER_NOT_TO_SAY", mockProfile.getGender());
    }

    @Test
    @DisplayName("User not found throws UserNotFoundException")
    void testUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> profileService.getProfile(999L));
    }
}
