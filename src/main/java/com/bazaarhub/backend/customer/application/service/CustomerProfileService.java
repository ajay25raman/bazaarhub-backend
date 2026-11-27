package com.bazaarhub.backend.customer.application.service;

import com.bazaarhub.backend.customer.application.dto.request.*;
import com.bazaarhub.backend.customer.application.dto.response.CustomerProfileResponse;
import com.bazaarhub.backend.customer.application.mapper.CustomerMapper;
import com.bazaarhub.backend.customer.domain.exception.CustomerProfileAlreadyExistsException;
import com.bazaarhub.backend.customer.domain.exception.CustomerProfileNotFoundException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.CustomerProfileEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataCustomerProfileRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class CustomerProfileService {

    private final SpringDataCustomerProfileRepository profileRepository;
    private final SpringDataUserRepository userRepository;

    public CustomerProfileService(SpringDataCustomerProfileRepository profileRepository,
                                  SpringDataUserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(Long userId) {
        UserEntity user = resolveUser(userId);
        CustomerProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomerProfileNotFoundException(userId));

        return CustomerMapper.toProfileResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getAccountProfile(Long userId) {
        UserEntity user = resolveUser(userId);
        CustomerProfileEntity profile = profileRepository.findByUserId(userId).orElse(null);
        return CustomerMapper.toProfileResponse(user, profile);
    }

    @Transactional
    public CustomerProfileResponse createProfile(Long userId, CustomerProfileCreateRequest request) {
        UserEntity user = resolveUser(userId);

        if (profileRepository.existsByUserId(userId)) {
            throw new CustomerProfileAlreadyExistsException("Customer profile already exists for user ID: " + userId);
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
            userRepository.save(user);
        }

        CustomerProfileEntity entity = CustomerProfileEntity.builder()
                .user(user)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender() != null ? request.getGender().toUpperCase() : null)
                .profileImageUrl(request.getProfileImageUrl())
                .createdBy(user)
                .updatedBy(user)
                .build();

        CustomerProfileEntity saved = profileRepository.save(entity);
        log.info("[CUSTOMER PROFILE CREATED] Created profile ID: {} for user ID: {}", saved.getId(), userId);
        return CustomerMapper.toProfileResponse(user, saved);
    }

    @Transactional
    public CustomerProfileResponse updateProfile(Long userId, CustomerProfileUpdateRequest request) {
        UserEntity user = resolveUser(userId);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        userRepository.save(user);

        CustomerProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> CustomerProfileEntity.builder()
                        .user(user)
                        .createdBy(user)
                        .build());

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null && !request.getGender().isBlank()) {
            profile.setGender(request.getGender().toUpperCase());
        }
        if (request.getProfileImageUrl() != null) {
            profile.setProfileImageUrl(request.getProfileImageUrl().isBlank() ? null : request.getProfileImageUrl().trim());
        }
        profile.setUpdatedBy(user);

        CustomerProfileEntity saved = profileRepository.save(profile);
        log.info("[CUSTOMER PROFILE UPDATED] Updated profile ID: {} for user ID: {}", saved.getId(), userId);
        return CustomerMapper.toProfileResponse(user, saved);
    }

    @Transactional
    public CustomerProfileResponse updateDateOfBirth(Long userId, LocalDate dateOfBirth) {
        UserEntity user = resolveUser(userId);
        CustomerProfileEntity profile = getOrCreateProfileEntity(user);
        profile.setDateOfBirth(dateOfBirth);
        profile.setUpdatedBy(user);

        CustomerProfileEntity saved = profileRepository.save(profile);
        log.info("[CUSTOMER PROFILE DOB UPDATED] User ID: {}, new DOB: {}", userId, dateOfBirth);
        return CustomerMapper.toProfileResponse(user, saved);
    }

    @Transactional
    public CustomerProfileResponse updateGender(Long userId, String gender) {
        UserEntity user = resolveUser(userId);
        CustomerProfileEntity profile = getOrCreateProfileEntity(user);
        profile.setGender(gender != null ? gender.toUpperCase() : null);
        profile.setUpdatedBy(user);

        CustomerProfileEntity saved = profileRepository.save(profile);
        log.info("[CUSTOMER PROFILE GENDER UPDATED] User ID: {}, new gender: {}", userId, gender);
        return CustomerMapper.toProfileResponse(user, saved);
    }

    @Transactional
    public CustomerProfileResponse updateProfileImageUrl(Long userId, String profileImageUrl) {
        UserEntity user = resolveUser(userId);
        CustomerProfileEntity profile = getOrCreateProfileEntity(user);
        profile.setProfileImageUrl(profileImageUrl != null && !profileImageUrl.isBlank() ? profileImageUrl.trim() : null);
        profile.setUpdatedBy(user);

        CustomerProfileEntity saved = profileRepository.save(profile);
        log.info("[CUSTOMER PROFILE IMAGE UPDATED] User ID: {}, image URL: {}", userId, profileImageUrl);
        return CustomerMapper.toProfileResponse(user, saved);
    }

    private CustomerProfileEntity getOrCreateProfileEntity(UserEntity user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> CustomerProfileEntity.builder()
                        .user(user)
                        .createdBy(user)
                        .build());
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
