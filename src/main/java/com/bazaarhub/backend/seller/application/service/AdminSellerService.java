package com.bazaarhub.backend.seller.application.service;

import com.bazaarhub.backend.identity.infrastructure.persistence.entity.RoleEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataRoleRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.bazaarhub.backend.seller.application.dto.request.AdminActionRequest;
import com.bazaarhub.backend.seller.application.dto.request.VerifyDocumentRequest;
import com.bazaarhub.backend.seller.application.dto.response.*;
import com.bazaarhub.backend.seller.domain.exception.DocumentNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.InvalidSellerStateException;
import com.bazaarhub.backend.seller.domain.exception.SellerNotFoundException;
import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.SellerApprovalStatus;
import com.bazaarhub.backend.seller.domain.model.SellerVerificationStatus;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerDocumentEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import com.bazaarhub.backend.shared.domain.exception.BusinessRuleViolationException;
import com.bazaarhub.backend.shared.domain.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AdminSellerService {

    private static final Logger log = LoggerFactory.getLogger(AdminSellerService.class);

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataUserRepository userRepository;
    private final SpringDataRoleRepository roleRepository;
    private final SpringDataSellerDocumentRepository documentRepository;
    private final SpringDataSellerBankAccountRepository bankAccountRepository;
    private final SpringDataStoreRepository storeRepository;
    private final SellerDocumentService documentService;
    private final SellerBankService bankService;

    public AdminSellerService(SpringDataSellerRepository sellerRepository,
                              SpringDataUserRepository userRepository,
                              SpringDataRoleRepository roleRepository,
                              SpringDataSellerDocumentRepository documentRepository,
                              SpringDataSellerBankAccountRepository bankAccountRepository,
                              SpringDataStoreRepository storeRepository,
                              SellerDocumentService documentService,
                              SellerBankService bankService) {
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.documentRepository = documentRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.storeRepository = storeRepository;
        this.documentService = documentService;
        this.bankService = bankService;
    }

    @Transactional(readOnly = true)
    public Page<AdminSellerSummaryResponse> getSellers(
            SellerApprovalStatus approvalStatus,
            SellerVerificationStatus verificationStatus,
            Pageable pageable) {

        Page<SellerEntity> page;
        if (approvalStatus != null && verificationStatus != null) {
            page = sellerRepository.findByApprovalStatusAndVerificationStatus(approvalStatus, verificationStatus, pageable);
        } else if (approvalStatus != null) {
            page = sellerRepository.findByApprovalStatus(approvalStatus, pageable);
        } else if (verificationStatus != null) {
            page = sellerRepository.findByVerificationStatus(verificationStatus, pageable);
        } else {
            page = sellerRepository.findAll(pageable);
        }

        return page.map(seller -> AdminSellerSummaryResponse.builder()
                .id(seller.getId())
                .userId(seller.getUser() != null ? seller.getUser().getId() : null)
                .businessName(seller.getBusinessName())
                .businessEmail(seller.getBusinessEmail())
                .taxIdentifier(seller.getTaxIdentifier())
                .verificationStatus(seller.getVerificationStatus())
                .approvalStatus(seller.getApprovalStatus())
                .storeName(seller.getStore() != null ? seller.getStore().getStoreName() : null)
                .createdAt(seller.getCreatedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public AdminSellerDetailResponse getSellerDetails(Long sellerId) {
        SellerEntity seller = sellerRepository.findByIdWithDetails(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        List<SellerDocumentResponse> documents = documentRepository.findBySellerId(sellerId).stream()
                .map(documentService::mapToDocumentResponse)
                .toList();

        List<SellerBankResponse> bankAccounts = bankAccountRepository.findBySellerId(sellerId).stream()
                .map(bankService::mapToBankResponseWithDecryptedMasking)
                .toList();

        StoreResponse storeResp = null;
        if (seller.getStore() != null) {
            storeResp = StoreService.mapToStoreResponse(seller.getStore());
        }

        UserEntity user = seller.getUser();

        return AdminSellerDetailResponse.builder()
                .id(seller.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .businessName(seller.getBusinessName())
                .businessEmail(seller.getBusinessEmail())
                .businessPhone(seller.getBusinessPhone())
                .taxIdentifier(seller.getTaxIdentifier())
                .verificationStatus(seller.getVerificationStatus())
                .approvalStatus(seller.getApprovalStatus())
                .approvedById(seller.getApprovedBy() != null ? seller.getApprovedBy().getId() : null)
                .approvedAt(seller.getApprovedAt())
                .rejectionReason(seller.getRejectionReason())
                .store(storeResp)
                .documents(documents)
                .bankAccounts(bankAccounts)
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }

    @Transactional
    public SellerResponse approveSeller(Long adminUserId, Long sellerId) {
        UserEntity adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(adminUserId)));

        SellerEntity seller = sellerRepository.findByIdWithDetails(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        // Security check: Administrators cannot approve their own seller applications
        if (seller.getUser() != null && seller.getUser().getId().equals(adminUserId)) {
            log.warn("[SECURITY VIOLATION] Administrator user ID {} attempted self-approval of seller application ID {}",
                    adminUserId, sellerId);
            throw new InvalidSellerStateException("Administrators cannot approve their own seller application");
        }

        if (seller.getApprovalStatus() == SellerApprovalStatus.APPROVED) {
            log.info("[SELLER APPROVAL] Seller ID: {} is already approved. Idempotent return.", sellerId);
            return SellerOnboardingService.mapToSellerResponse(seller);
        }

        seller.setApprovalStatus(SellerApprovalStatus.APPROVED);
        seller.setApprovedBy(adminUser);
        seller.setApprovedAt(Instant.now());
        seller.setRejectionReason(null);
        seller.setUpdatedBy(adminUser);

        // Assign ROLE_SELLER to user while preserving existing customer role
        UserEntity sellerUser = seller.getUser();
        if (sellerUser != null) {
            RoleEntity sellerRole = roleRepository.findByName("ROLE_SELLER")
                    .or(() -> roleRepository.findByName("SELLER"))
                    .orElseGet(() -> {
                        log.warn("[SELLER APPROVAL] ROLE_SELLER not found in DB. Creating dynamically for assignment.");
                        return roleRepository.save(RoleEntity.builder()
                                .name("ROLE_SELLER")
                                .description("Merchant selling products on BazaarHub")
                                .build());
                    });

            if (!sellerUser.getRoles().contains(sellerRole)) {
                sellerUser.getRoles().add(sellerRole);
                userRepository.save(sellerUser);
                log.info("[SELLER APPROVAL] Assigned ROLE_SELLER to user ID: {}", sellerUser.getId());
            }
        }

        // If seller created a storefront that was pending approval, activate it
        if (seller.getStore() != null && seller.getStore().getStatus() == StoreStatus.INACTIVE) {
            seller.getStore().setStatus(StoreStatus.ACTIVE);
            seller.getStore().setUpdatedBy(adminUser);
            storeRepository.save(seller.getStore());
            log.info("[STORE ACTIVATION] Activated storefront ID: {} upon seller approval", seller.getStore().getId());
        }

        SellerEntity saved = sellerRepository.save(seller);
        log.info("[SELLER APPROVED] Seller ID: {} approved successfully by administrator ID: {}", sellerId, adminUserId);

        return SellerOnboardingService.mapToSellerResponse(saved);
    }

    @Transactional
    public SellerResponse rejectSeller(Long adminUserId, Long sellerId, AdminActionRequest request) {
        UserEntity adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(adminUserId)));

        SellerEntity seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessRuleViolationException("A valid rejection reason must be provided when rejecting a seller application");
        }

        seller.setApprovalStatus(SellerApprovalStatus.REJECTED);
        seller.setRejectionReason(request.getReason().trim());
        seller.setUpdatedBy(adminUser);

        SellerEntity saved = sellerRepository.save(seller);
        log.info("[SELLER REJECTED] Seller ID: {} rejected by admin ID: {}. Reason: '{}'",
                sellerId, adminUserId, request.getReason().trim());

        return SellerOnboardingService.mapToSellerResponse(saved);
    }

    @Transactional
    public SellerResponse suspendSeller(Long adminUserId, Long sellerId, AdminActionRequest request) {
        UserEntity adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(adminUserId)));

        SellerEntity seller = sellerRepository.findByIdWithDetails(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        seller.setApprovalStatus(SellerApprovalStatus.SUSPENDED);
        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason().trim()
                : "Suspended by administrator action";
        seller.setRejectionReason(reason);
        seller.setUpdatedBy(adminUser);

        // Suspend storefront as well
        if (seller.getStore() != null) {
            seller.getStore().setStatus(StoreStatus.SUSPENDED);
            seller.getStore().setUpdatedBy(adminUser);
            storeRepository.save(seller.getStore());
            log.info("[STORE SUSPENDED] Suspended storefront ID: {} for suspended seller ID: {}",
                    seller.getStore().getId(), sellerId);
        }

        SellerEntity saved = sellerRepository.save(seller);
        log.info("[SELLER SUSPENDED] Seller ID: {} suspended by admin ID: {}", sellerId, adminUserId);

        return SellerOnboardingService.mapToSellerResponse(saved);
    }

    @Transactional
    public SellerDocumentResponse verifyDocument(Long adminUserId, Long sellerId, VerifyDocumentRequest request) {
        UserEntity adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", String.valueOf(adminUserId)));

        SellerEntity seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(sellerId));

        SellerDocumentEntity document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(request.getDocumentId()));

        if (!document.getSeller().getId().equals(sellerId)) {
            throw new BusinessRuleViolationException("Document ID " + request.getDocumentId() + " does not belong to seller ID " + sellerId);
        }

        document.setVerificationStatus(request.getStatus());
        document.setReviewedBy(adminUser);
        document.setReviewedAt(Instant.now());
        if (request.getStatus() == DocumentVerificationStatus.REJECTED) {
            document.setRejectionReason(request.getRejectionReason());
        } else {
            document.setRejectionReason(null);
        }
        document.setUpdatedBy(adminUser);

        SellerDocumentEntity savedDoc = documentRepository.save(document);

        // Check if all documents are now verified
        List<SellerDocumentEntity> allDocs = documentRepository.findBySellerId(sellerId);
        boolean allVerified = !allDocs.isEmpty() && allDocs.stream()
                .allMatch(d -> d.getVerificationStatus() == DocumentVerificationStatus.VERIFIED);

        if (allVerified) {
            seller.setVerificationStatus(SellerVerificationStatus.VERIFIED);
            sellerRepository.save(seller);
            log.info("[KYC VERIFIED] All KYC documents verified. Seller ID: {} verification status marked VERIFIED", sellerId);
        }

        log.info("[DOCUMENT REVIEW] Document ID: {} set to {} by admin ID: {}",
                savedDoc.getId(), request.getStatus(), adminUserId);

        return documentService.mapToDocumentResponse(savedDoc);
    }
}
