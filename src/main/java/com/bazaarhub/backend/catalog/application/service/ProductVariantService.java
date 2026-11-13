package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductVariantUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductVariantResponse;
import com.bazaarhub.backend.catalog.application.mapper.ProductMapper;
import com.bazaarhub.backend.catalog.domain.exception.InvalidProductStateException;
import com.bazaarhub.backend.catalog.domain.exception.ProductVariantNotFoundException;
import com.bazaarhub.backend.catalog.domain.exception.SkuAlreadyExistsException;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductVariantRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductVariantService {

    private static final Logger log = LoggerFactory.getLogger(ProductVariantService.class);

    private final SpringDataProductVariantRepository variantRepository;
    private final ProductService productService;
    private final SpringDataUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ProductVariantService(SpringDataProductVariantRepository variantRepository,
                                 ProductService productService,
                                 SpringDataUserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.variantRepository = variantRepository;
        this.productService = productService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProductVariantResponse createVariant(Long userId, Long productId, ProductVariantCreateRequest request) {
        ProductEntity product = productService.getProductAndValidateOwnership(productId, userId);

        String cleanSku = request.getSku().trim().toUpperCase();

        // 1. Check SKU uniqueness
        if (variantRepository.existsBySku(cleanSku)) {
            throw new SkuAlreadyExistsException("SKU '" + cleanSku + "' already exists");
        }

        // 2. Validate prices (MRP >= Selling Price)
        validatePrices(request.getSellingPrice(), request.getMrp());

        // 3. Validate attributes JSON
        validateJsonAttributes(request.getAttributes());

        UserEntity user = userRepository.getReferenceById(userId);

        ProductVariantEntity variant = ProductVariantEntity.builder()
                .product(product)
                .sku(cleanSku)
                .barcode(request.getBarcode() != null ? request.getBarcode().trim() : null)
                .variantName(request.getVariantName().trim())
                .attributes(request.getAttributes())
                .sellingPrice(request.getSellingPrice())
                .mrp(request.getMrp())
                .taxRate(request.getTaxRate() != null ? request.getTaxRate() : BigDecimal.ZERO)
                .weight(request.getWeight())
                .status(request.getStatus() != null ? request.getStatus() : CatalogStatus.ACTIVE)
                .createdBy(user)
                .updatedBy(user)
                .build();

        ProductVariantEntity saved = variantRepository.save(variant);
        log.info("Created variant ID: {} with SKU: '{}' for product ID: {}", saved.getId(), cleanSku, productId);
        return ProductMapper.toVariantResponse(saved);
    }

    @Transactional
    public ProductVariantResponse updateVariant(Long userId, Long productId, Long variantId, ProductVariantUpdateRequest request) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductVariantEntity variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant ID " + variantId + " not found on product ID " + productId));

        String cleanSku = request.getSku().trim().toUpperCase();

        // 1. Check SKU uniqueness if changed
        if (!cleanSku.equalsIgnoreCase(variant.getSku()) && variantRepository.existsBySkuAndIdNot(cleanSku, variantId)) {
            throw new SkuAlreadyExistsException("SKU '" + cleanSku + "' is already in use by another variant");
        }

        // 2. Validate prices
        validatePrices(request.getSellingPrice(), request.getMrp());

        // 3. Validate attributes JSON
        validateJsonAttributes(request.getAttributes());

        variant.setSku(cleanSku);
        variant.setBarcode(request.getBarcode() != null ? request.getBarcode().trim() : null);
        variant.setVariantName(request.getVariantName().trim());
        variant.setAttributes(request.getAttributes());
        variant.setSellingPrice(request.getSellingPrice());
        variant.setMrp(request.getMrp());
        if (request.getTaxRate() != null) {
            variant.setTaxRate(request.getTaxRate());
        }
        variant.setWeight(request.getWeight());
        variant.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductVariantEntity saved = variantRepository.save(variant);
        log.info("Updated variant ID: {} on product ID: {}", variantId, productId);
        return ProductMapper.toVariantResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getVariants(Long productId) {
        return variantRepository.findByProductId(productId)
                .stream()
                .map(ProductMapper::toVariantResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductVariantResponse getVariant(Long productId, Long variantId) {
        ProductVariantEntity variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant ID " + variantId + " not found on product ID " + productId));
        return ProductMapper.toVariantResponse(variant);
    }

    @Transactional
    public ProductVariantResponse updateVariantStatus(Long userId, Long productId, Long variantId, CatalogStatus status) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductVariantEntity variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant ID " + variantId + " not found on product ID " + productId));

        variant.setStatus(status);
        variant.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductVariantEntity saved = variantRepository.save(variant);
        log.info("Updated status to {} for variant ID: {} on product ID: {}", status, variantId, productId);
        return ProductMapper.toVariantResponse(saved);
    }

    @Transactional
    public void deleteVariant(Long userId, Long productId, Long variantId) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductVariantEntity variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ProductVariantNotFoundException("Variant ID " + variantId + " not found on product ID " + productId));

        variantRepository.delete(variant);
        log.info("Deleted variant ID: {} from product ID: {}", variantId, productId);
    }

    private void validatePrices(BigDecimal sellingPrice, BigDecimal mrp) {
        if (sellingPrice == null || mrp == null) {
            throw new InvalidProductStateException("Both selling price and MRP are mandatory");
        }
        if (mrp.compareTo(sellingPrice) < 0) {
            throw new InvalidProductStateException("MRP (" + mrp + ") must be greater than or equal to selling price (" + sellingPrice + ")");
        }
    }

    private void validateJsonAttributes(String attributes) {
        if (attributes == null || attributes.isBlank()) {
            return;
        }
        try {
            objectMapper.readTree(attributes);
        } catch (JsonProcessingException e) {
            throw new InvalidProductStateException("Attributes must be a valid JSON string: " + e.getOriginalMessage());
        }
    }
}
