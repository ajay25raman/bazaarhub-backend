package com.bazaarhub.backend.seller.application.service;

import com.bazaarhub.backend.seller.application.dto.request.SellerBankRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerBankResponse;
import com.bazaarhub.backend.seller.domain.exception.BankAccountNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.SellerNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.UnauthorizedResourceAccessException;
import com.bazaarhub.backend.seller.domain.model.BankVerificationStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerBankAccountEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerBankAccountRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.shared.infrastructure.security.AesGcmEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SellerBankService {

    private static final Logger log = LoggerFactory.getLogger(SellerBankService.class);

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataSellerBankAccountRepository bankAccountRepository;
    private final AesGcmEncryptionService encryptionService;

    public SellerBankService(SpringDataSellerRepository sellerRepository,
                             SpringDataSellerBankAccountRepository bankAccountRepository,
                             AesGcmEncryptionService encryptionService) {
        this.sellerRepository = sellerRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public SellerBankResponse addBankAccount(Long userId, SellerBankRequest request) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        // If marked primary, unset existing primary accounts for this seller
        if (request.isPrimary()) {
            bankAccountRepository.findBySellerIdAndIsPrimaryTrue(seller.getId())
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setPrimary(false);
                        bankAccountRepository.save(existingPrimary);
                    });
        }

        // Encrypt sensitive account number and IFSC code using AES-256-GCM
        String encryptedAccount = encryptionService.encrypt(request.getAccountNumber().trim());
        String encryptedIfsc = encryptionService.encrypt(request.getIfscCode().trim().toUpperCase());

        SellerBankAccountEntity bankAccount = SellerBankAccountEntity.builder()
                .seller(seller)
                .accountHolderName(request.getAccountHolderName().trim())
                .encryptedAccountNumber(encryptedAccount)
                .encryptedIfscCode(encryptedIfsc)
                .bankName(request.getBankName() != null ? request.getBankName().trim() : null)
                .branchName(request.getBranchName() != null ? request.getBranchName().trim() : null)
                .verificationStatus(BankVerificationStatus.PENDING)
                .isPrimary(request.isPrimary())
                .createdBy(seller.getUser())
                .updatedBy(seller.getUser())
                .build();

        SellerBankAccountEntity saved = bankAccountRepository.save(bankAccount);
        log.info("[SELLER BANK] Encrypted and saved bank account ID: {} for seller ID: {}", saved.getId(), seller.getId());

        // Return safely masked representation only
        return mapToBankResponse(saved, request.getAccountNumber().trim(), request.getIfscCode().trim().toUpperCase());
    }

    @Transactional(readOnly = true)
    public List<SellerBankResponse> getBankAccounts(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        return bankAccountRepository.findBySellerId(seller.getId()).stream()
                .map(this::mapToBankResponseWithDecryptedMasking)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerBankResponse getBankAccountById(Long userId, Long accountId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        SellerBankAccountEntity account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BankAccountNotFoundException(accountId));

        if (!account.getSeller().getId().equals(seller.getId())) {
            log.warn("[SECURITY VIOLATION] User ID {} attempted to access bank account ID {} owned by seller ID {}",
                    userId, accountId, account.getSeller().getId());
            throw new UnauthorizedResourceAccessException("You are not authorized to access this bank account");
        }

        return mapToBankResponseWithDecryptedMasking(account);
    }

    private SellerBankResponse mapToBankResponse(SellerBankAccountEntity account, String plainAccount, String plainIfsc) {
        return SellerBankResponse.builder()
                .id(account.getId())
                .sellerId(account.getSeller() != null ? account.getSeller().getId() : null)
                .accountHolderName(account.getAccountHolderName())
                .maskedAccountNumber(encryptionService.maskAccountNumber(plainAccount))
                .maskedIfscCode(encryptionService.maskIfscCode(plainIfsc))
                .bankName(account.getBankName())
                .branchName(account.getBranchName())
                .verificationStatus(account.getVerificationStatus())
                .isPrimary(account.isPrimary())
                .createdAt(account.getCreatedAt())
                .build();
    }

    public SellerBankResponse mapToBankResponseWithDecryptedMasking(SellerBankAccountEntity account) {
        String plainAccount = encryptionService.decrypt(account.getEncryptedAccountNumber());
        String plainIfsc = encryptionService.decrypt(account.getEncryptedIfscCode());
        return mapToBankResponse(account, plainAccount, plainIfsc);
    }
}
