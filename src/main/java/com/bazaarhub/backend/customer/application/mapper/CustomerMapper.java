package com.bazaarhub.backend.customer.application.mapper;

import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.customer.application.dto.response.CustomerProfileResponse;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.CustomerProfileEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerProfileResponse toProfileResponse(UserEntity user, CustomerProfileEntity profile) {
        if (user == null) {
            return null;
        }

        CustomerProfileResponse.CustomerProfileResponseBuilder builder = CustomerProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified());

        if (profile != null) {
            builder.id(profile.getId())
                    .dateOfBirth(profile.getDateOfBirth())
                    .gender(profile.getGender())
                    .profileImageUrl(profile.getProfileImageUrl())
                    .createdAt(profile.getCreatedAt())
                    .updatedAt(profile.getUpdatedAt());
        } else {
            builder.createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt());
        }

        return builder.build();
    }

    public static AddressResponse toAddressResponse(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        return AddressResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .addressType(entity.getAddressType())
                .recipientName(entity.getRecipientName())
                .phone(entity.getPhone())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .landmark(entity.getLandmark())
                .city(entity.getCity())
                .state(entity.getState())
                .postalCode(entity.getPostalCode())
                .country(entity.getCountry())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .isDefault(entity.isDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
