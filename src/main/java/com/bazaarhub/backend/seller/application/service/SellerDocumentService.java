package com.bazaarhub.backend.seller.application.service;

import com.bazaarhub.backend.seller.application.dto.request.SellerDocumentRequest;
import com.bazaarhub.backend.seller.application.dto.response.SellerDocumentResponse;
import com.bazaarhub.backend.seller.domain.exception.DocumentNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.SellerNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.UnauthorizedResourceAccessException;
import com.bazaarhub.backend.seller.domain.model.DocumentVerificationStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerDocumentEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerDocumentRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SellerDocumentService {

    private static final Logger log = LoggerFactory.getLogger(SellerDocumentService.class);

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataSellerDocumentRepository documentRepository;

    public SellerDocumentService(SpringDataSellerRepository sellerRepository,
                                 SpringDataSellerDocumentRepository documentRepository) {
        this.sellerRepository = sellerRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional
    public SellerDocumentResponse uploadDocument(Long userId, SellerDocumentRequest request) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        SellerDocumentEntity document = SellerDocumentEntity.builder()
                .seller(seller)
                .documentType(request.getDocumentType().trim().toUpperCase())
                .documentNumber(request.getDocumentNumber() != null ? request.getDocumentNumber().trim() : null)
                .documentUrl(request.getDocumentUrl().trim())
                .verificationStatus(DocumentVerificationStatus.PENDING)
                .createdBy(seller.getUser())
                .updatedBy(seller.getUser())
                .build();

        SellerDocumentEntity saved = documentRepository.save(document);
        log.info("[SELLER DOCUMENT] Registered KYC document ID: {} for seller ID: {}", saved.getId(), seller.getId());

        return mapToDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SellerDocumentResponse> getDocuments(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        return documentRepository.findBySellerId(seller.getId()).stream()
                .map(this::mapToDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SellerDocumentResponse getDocumentById(Long userId, Long documentId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        SellerDocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getSeller().getId().equals(seller.getId())) {
            log.warn("[SECURITY VIOLATION] User ID {} attempted to access document ID {} owned by seller ID {}",
                    userId, documentId, document.getSeller().getId());
            throw new UnauthorizedResourceAccessException("You are not authorized to access this document");
        }

        return mapToDocumentResponse(document);
    }

    public SellerDocumentResponse mapToDocumentResponse(SellerDocumentEntity doc) {
        return SellerDocumentResponse.builder()
                .id(doc.getId())
                .sellerId(doc.getSeller() != null ? doc.getSeller().getId() : null)
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .documentUrl(doc.getDocumentUrl())
                .verificationStatus(doc.getVerificationStatus())
                .rejectionReason(doc.getRejectionReason())
                .reviewedAt(doc.getReviewedAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
