package com.bazaarhub.backend.seller.application.service;

import com.bazaarhub.backend.seller.application.dto.request.StoreCreateRequest;
import com.bazaarhub.backend.seller.application.dto.request.StoreUpdateRequest;
import com.bazaarhub.backend.seller.application.dto.response.StoreResponse;
import com.bazaarhub.backend.seller.domain.exception.SellerNotFoundException;
import com.bazaarhub.backend.seller.domain.exception.StoreAlreadyExistsException;
import com.bazaarhub.backend.seller.domain.exception.StoreNotFoundException;
import com.bazaarhub.backend.seller.domain.model.StoreStatus;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.SellerEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.entity.StoreEntity;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataSellerRepository;
import com.bazaarhub.backend.seller.infrastructure.persistence.repository.SpringDataStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private final SpringDataSellerRepository sellerRepository;
    private final SpringDataStoreRepository storeRepository;

    public StoreService(SpringDataSellerRepository sellerRepository,
                        SpringDataStoreRepository storeRepository) {
        this.sellerRepository = sellerRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreResponse createStore(Long userId, StoreCreateRequest request) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        if (storeRepository.existsBySellerId(seller.getId())) {
            throw new StoreAlreadyExistsException("A storefront already exists for this seller account");
        }

        String rawSlug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : toSlug(request.getStoreName());

        if (storeRepository.existsBySlug(rawSlug)) {
            throw new StoreAlreadyExistsException("Store slug '" + rawSlug + "' is already in use by another merchant");
        }

        StoreEntity store = StoreEntity.builder()
                .seller(seller)
                .storeName(request.getStoreName().trim())
                .slug(rawSlug)
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl() != null ? request.getLogoUrl().trim() : null)
                .bannerUrl(request.getBannerUrl() != null ? request.getBannerUrl().trim() : null)
                .status(StoreStatus.INACTIVE) // Default INACTIVE until approved
                .createdBy(seller.getUser())
                .updatedBy(seller.getUser())
                .build();

        StoreEntity saved = storeRepository.save(store);
        log.info("[STORE CREATED] New storefront ID: {} with slug: '{}' for seller ID: {}",
                saved.getId(), saved.getSlug(), seller.getId());

        return mapToStoreResponse(saved);
    }

    @Transactional(readOnly = true)
    public StoreResponse getMyStore(Long userId) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        StoreEntity store = storeRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new StoreNotFoundException("No storefront configured for seller ID: " + seller.getId()));

        return mapToStoreResponse(store);
    }

    @Transactional
    public StoreResponse updateStore(Long userId, StoreUpdateRequest request) {
        SellerEntity seller = sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller profile associated with user ID: " + userId));

        StoreEntity store = storeRepository.findBySellerId(seller.getId())
                .orElseThrow(() -> new StoreNotFoundException("No storefront configured for seller ID: " + seller.getId()));

        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            store.setStoreName(request.getStoreName().trim());
        }
        if (request.getDescription() != null) {
            store.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            store.setLogoUrl(request.getLogoUrl().trim());
        }
        if (request.getBannerUrl() != null) {
            store.setBannerUrl(request.getBannerUrl().trim());
        }

        store.setUpdatedBy(seller.getUser());
        StoreEntity saved = storeRepository.save(store);
        log.info("[STORE UPDATED] Updated storefront ID: {} for seller ID: {}", saved.getId(), seller.getId());

        return mapToStoreResponse(saved);
    }

    public static StoreResponse mapToStoreResponse(StoreEntity store) {
        return StoreResponse.builder()
                .id(store.getId())
                .sellerId(store.getSeller() != null ? store.getSeller().getId() : null)
                .storeName(store.getStoreName())
                .slug(store.getSlug())
                .description(store.getDescription())
                .logoUrl(store.getLogoUrl())
                .bannerUrl(store.getBannerUrl())
                .status(store.getStatus())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "store-" + System.currentTimeMillis();
        }
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("").toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("-+", "-").replaceAll("^-|-$", "");
        return slug.isBlank() ? "store-" + System.currentTimeMillis() : slug;
    }
}
