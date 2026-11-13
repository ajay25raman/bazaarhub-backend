package com.bazaarhub.backend.seller.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.application.dto.request.SellerOnboardingRequest;
import com.bazaarhub.backend.seller.application.dto.request.UpdateSellerProfileRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerResponse;
import com.bazaarhub.backend.seller.application.dto.response.SellerStatusResponse;
import com.bazaarhub.backend.seller.application.dto.response.StoreResponse;
import com.bazaarhub.backend.seller.domain.exception.SellerAlreadyExistsException;
import com.bazaarhub.backend.seller.domain.exception.SellerNotFoundException;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerOnboardingService {

    private static final Logger log = LoggerFactory.getLogger(SellerOnboardingService.class);

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataStoreRepository storeRepository;
    private final SpringDataSellerDocumentRepository documentRepository;
    private final SpringDataSellerBankAccountRepository bankAccountRepository;

    public SellerOnboardingService(SpringDataSellerRepository sellerRepository,
                                   SpringDataUserRepository userRepository,
                                   SpringDataStoreRepository storeRepository,
                                   SpringDataSellerDocumentRepository documentRepository,
                                   SpringDataSellerBankAccountRepository bankAccountRepository) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.documentRepository = documentRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public SellerResponse submitOnboarding(Long userId, SellerOnboardingRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(userId)));

        if (sellerRepository.existsByUserId(userId)) {
            throw new SellerAlreadyExistsException("A seller application or profile already exists for this user account");
        }

        String businessEmail = request.getBusinessEmail() != null && !request.getBusinessEmail().isBlank()
                ? request.getBusinessEmail().trim().toLowerCase()
                : user.getEmail();

        String businessPhone = request.getBusinessPhone() != null && !request.getBusinessPhone().isBlank()
                ? request.getBusinessPhone().trim()
                : user.getPhone();

        SellerEntity seller = SellerEntity.builder()
                .user(user)
                .businessName(request.getBusinessName().trim())
                .businessEmail(businessEmail)
                .businessPhone(businessPhone)
                .taxIdentifier(request.getTaxIdentifier() != null ? request.getTaxIdentifier().trim() : null)
                .verificationStatus(SellerVerificationStatus.PENDING)
                .approvalStatus(SellerApprovalStatus.PENDING)
                .createdBy(user)
                .updatedBy(user)
                .build();

        SellerEntity saved = sellerRepository.save(seller);
        log.info("[SELLER ONBOARDING] Application submitted successfully by user ID: {}, assigned seller ID: {}",
                userId, saved.getId());

        return mapToSellerResponse(saved);
    }

    @Transactional(readOnly = true)
    public SellerResponse getSellerProfile(Long userId) {
        SellerEntity seller = sellerRepository.findByUserIdWithDetails(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));
        return mapToSellerResponse(seller);
    }

    @Transactional
    public SellerResponse updateSellerProfile(Long userId, UpdateSellerProfileRequest request) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        if (request.getBusinessName() != null && !request.getBusinessName().isBlank()) {
            seller.setBusinessName(request.getBusinessName().trim());
        }
        if (request.getBusinessEmail() != null && !request.getBusinessEmail().isBlank()) {
            seller.setBusinessEmail(request.getBusinessEmail().trim().toLowerCase());
        }
        if (request.getBusinessPhone() != null && !request.getBusinessPhone().isBlank()) {
            seller.setBusinessPhone(request.getBusinessPhone().trim());
        }
        if (request.getTaxIdentifier() != null) {
            seller.setTaxIdentifier(request.getTaxIdentifier().trim());
        }

        seller.setUpdatedBy(seller.getUser());
        SellerEntity saved = sellerRepository.save(seller);
        log.info("[SELLER PROFILE] Updated business profile for seller ID: {}", saved.getId());

        return mapToSellerResponse(saved);
    }

    @Transactional(readOnly = true)
    public SellerStatusResponse getSellerStatus(Long userId) {
        SellerEntity seller = sellerRepository.findByUserIdWithDetails(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        boolean hasStore = seller.getStore() != null;
        StoreStatus storeStatus = hasStore ? seller.getStore().getStatus() : null;
        int docCount = (int) documentRepository.countBySellerId(seller.getId());
        boolean hasBank = bankAccountRepository.existsBySellerId(seller.getId());
        boolean canSell = seller.getApprovalStatus() == SellerApprovalStatus.APPROVED &&
                          hasStore && storeStatus == StoreStatus.ACTIVE;

        return SellerStatusResponse.builder()
                .sellerId(seller.getId())
                .businessName(seller.getBusinessName())
                .verificationStatus(seller.getVerificationStatus())
                .approvalStatus(seller.getApprovalStatus())
                .hasStore(hasStore)
                .storeStatus(storeStatus)
                .documentsCount(docCount)
                .bankAccountConfigured(hasBank)
                .canSell(canSell)
                .rejectionReason(seller.getRejectionReason())
                .createdAt(seller.getCreatedAt())
                .approvedAt(seller.getApprovedAt())
                .build();
    }

    public static SellerResponse mapToSellerResponse(SellerEntity seller) {
        StoreResponse storeResp = null;
        if (seller.getStore() != null) {
            storeResp = StoreResponse.builder()
                    .id(seller.getStore().getId())
                    .sellerId(seller.getId())
                    .storeName(seller.getStore().getStoreName())
                    .slug(seller.getStore().getSlug())
                    .description(seller.getStore().getDescription())
                    .logoUrl(seller.getStore().getLogoUrl())
                    .bannerUrl(seller.getStore().getBannerUrl())
                    .status(seller.getStore().getStatus())
                    .createdAt(seller.getStore().getCreatedAt())
                    .updatedAt(seller.getStore().getUpdatedAt())
                    .build();
        }

        return SellerResponse.builder()
                .id(seller.getId())
                .userId(seller.getUser() != null ? seller.getUser().getId() : null)
                .businessName(seller.getBusinessName())
                .businessEmail(seller.getBusinessEmail())
                .businessPhone(seller.getBusinessPhone())
                .taxIdentifier(seller.getTaxIdentifier())
                .verificationStatus(seller.getVerificationStatus())
                .approvalStatus(seller.getApprovalStatus())
                .approvedAt(seller.getApprovedAt())
                .rejectionReason(seller.getRejectionReason())
                .store(storeResp)
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }
}
