package com.bazaarhub.backend.customer.application.service;

import com.bazaarhub.backend.customer.application.dto.request.AddressCreateRequest;
import com.bazaarhub.backend.customer.application.dto.request.AddressUpdateRequest;
import com.bazaarhub.backend.customer.application.dto.response.AddressResponse;
import com.bazaarhub.backend.customer.application.mapper.CustomerMapper;
import com.bazaarhub.backend.customer.domain.exception.AddressInUseException;
import com.bazaarhub.backend.customer.domain.exception.AddressNotFoundException;
import com.bazaarhub.backend.customer.domain.exception.UnauthorizedAddressAccessException;
import com.bazaarhub.backend.customer.infrastructure.persistence.entity.AddressEntity;
import com.bazaarhub.backend.customer.infrastructure.persistence.repository.SpringDataAddressRepository;
import com.bazaarhub.backend.identity.domain.exception.UserNotFoundException;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CustomerAddressService {

    private final SpringDataAddressRepository addressRepository;
    private final SpringDataUserRepository userRepository;

    public CustomerAddressService(SpringDataAddressRepository addressRepository,
                                  SpringDataUserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressCreateRequest request) {
        UserEntity user = resolveUser(userId);
        long existingCount = addressRepository.countByUserId(userId);

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault()) || existingCount == 0;

        if (makeDefault) {
            addressRepository.clearDefaultAddress(userId);
        }

        AddressEntity entity = AddressEntity.builder()
                .user(user)
                .addressType(request.getAddressType() != null && !request.getAddressType().isBlank() ? request.getAddressType().toUpperCase() : "HOME")
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .addressLine1(request.getAddressLine1().trim())
                .addressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null)
                .landmark(request.getLandmark() != null ? request.getLandmark().trim() : null)
                .city(request.getCity().trim())
                .state(request.getState().trim())
                .postalCode(request.getPostalCode().trim())
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry().trim() : "India")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(makeDefault)
                .createdBy(user)
                .updatedBy(user)
                .build();

        AddressEntity saved = addressRepository.save(entity);
        log.info("[ADDRESS CREATED] Created address ID: {} for user ID: {}, isDefault: {}", saved.getId(), userId, makeDefault);
        return CustomerMapper.toAddressResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getCustomerAddresses(Long userId) {
        resolveUser(userId);
        return addressRepository.findByUserIdOrderByIsDefaultDescUpdatedAtDesc(userId).stream()
                .map(CustomerMapper::toAddressResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AddressResponse> getCustomerAddressesPaged(Long userId, Pageable pageable) {
        resolveUser(userId);
        return addressRepository.findByUserId(userId, pageable)
                .map(CustomerMapper::toAddressResponse);
    }

    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long userId, Long addressId) {
        AddressEntity address = getAddressAndValidateOwnership(addressId, userId);
        return CustomerMapper.toAddressResponse(address);
    }

    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        resolveUser(userId);
        AddressEntity defaultAddress = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new AddressNotFoundException("No default address configured for user ID: " + userId));

        return CustomerMapper.toAddressResponse(defaultAddress);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressUpdateRequest request) {
        UserEntity user = resolveUser(userId);
        AddressEntity address = getAddressAndValidateOwnership(addressId, userId);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            addressRepository.clearDefaultAddress(userId);
            address.setDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && address.isDefault()) {
            address.setDefault(false);
        }

        if (request.getAddressType() != null && !request.getAddressType().isBlank()) {
            address.setAddressType(request.getAddressType().toUpperCase());
        }
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddressLine1(request.getAddressLine1().trim());
        address.setAddressLine2(request.getAddressLine2() != null ? request.getAddressLine2().trim() : null);
        address.setLandmark(request.getLandmark() != null ? request.getLandmark().trim() : null);
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPostalCode(request.getPostalCode().trim());
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            address.setCountry(request.getCountry().trim());
        }
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setUpdatedBy(user);

        AddressEntity saved = addressRepository.save(address);
        log.info("[ADDRESS UPDATED] Updated address ID: {} for user ID: {}", saved.getId(), userId);
        return CustomerMapper.toAddressResponse(saved);
    }

    @Transactional
    public AddressResponse setDefaultAddress(Long userId, Long addressId) {
        AddressEntity address = getAddressAndValidateOwnership(addressId, userId);

        if (!address.isDefault()) {
            addressRepository.clearDefaultAddress(userId);
            address.setDefault(true);
            address = addressRepository.save(address);
            log.info("[ADDRESS SET DEFAULT] Address ID: {} promoted to default for user ID: {}", addressId, userId);
        }
        return CustomerMapper.toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        AddressEntity address = getAddressAndValidateOwnership(addressId, userId);
        boolean wasDefault = address.isDefault();

        try {
            addressRepository.delete(address);
            addressRepository.flush();
            log.info("[ADDRESS DELETED] Deleted address ID: {} for user ID: {}", addressId, userId);
        } catch (DataIntegrityViolationException ex) {
            log.error("[ADDRESS DELETE FAILED] Address ID: {} is in use by orders: {}", addressId, ex.getMessage());
            throw new AddressInUseException("Address ID " + addressId + " cannot be deleted as it is referenced by existing orders.");
        }

        if (wasDefault) {
            addressRepository.findFirstByUserIdOrderByUpdatedAtDesc(userId).ifPresent(remaining -> {
                remaining.setDefault(true);
                addressRepository.save(remaining);
                log.info("[ADDRESS DEFAULT PROMOTED] Remaining address ID: {} promoted to default for user ID: {}", remaining.getId(), userId);
            });
        }
    }

    private AddressEntity getAddressAndValidateOwnership(Long addressId, Long userId) {
        AddressEntity address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId));

        if (!address.getUser().getId().equals(userId)) {
            log.warn("[SECURITY VIOLATION] User ID {} attempted to access address ID {} owned by user ID {}",
                    userId, addressId, address.getUser().getId());
            throw new UnauthorizedAddressAccessException("You do not have permission to access address ID: " + addressId);
        }

        return address;
    }

    private UserEntity resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }
}
