package com.bazaarhub.backend.catalog.application.service;

import com.bazaarhub.backend.catalog.application.dto.request.ProductImageRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductImageResponse;
import com.bazaarhub.backend.catalog.application.mapper.ProductMapper;
import com.bazaarhub.backend.catalog.domain.exception.ProductImageNotFoundException;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.entity.ProductImageEntity;
import com.bazaarhub.backend.catalog.infrastructure.persistence.repository.SpringDataProductImageRepository;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.repository.SpringDataUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductImageService {

    private static final Logger log = LoggerFactory.getLogger(ProductImageService.class);

    private final SpringDataProductImageRepository imageRepository;
    private final ProductService productService;
    private final SpringDataUserRepository userRepository;

    public ProductImageService(SpringDataProductImageRepository imageRepository,
                               ProductService productService,
                               SpringDataUserRepository userRepository) {
        this.imageRepository = imageRepository;
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProductImageResponse addImage(Long userId, Long productId, ProductImageRequest request) {
        ProductEntity product = productService.getProductAndValidateOwnership(productId, userId);

        boolean hasExistingImages = imageRepository.existsByProductId(productId);
        boolean shouldBePrimary = Boolean.TRUE.equals(request.getIsPrimary()) || !hasExistingImages;

        if (shouldBePrimary && hasExistingImages) {
            imageRepository.resetPrimaryFlags(productId);
        }

        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : (int) imageRepository.countByProductId(productId);

        UserEntity user = userRepository.getReferenceById(userId);

        ProductImageEntity image = ProductImageEntity.builder()
                .product(product)
                .imageUrl(request.getImageUrl().trim())
                .altText(request.getAltText() != null ? request.getAltText().trim() : null)
                .displayOrder(displayOrder)
                .isPrimary(shouldBePrimary)
                .createdBy(user)
                .updatedBy(user)
                .build();

        ProductImageEntity saved = imageRepository.save(image);
        log.info("Added image ID: {} to product ID: {} (isPrimary={})", saved.getId(), productId, shouldBePrimary);
        return ProductMapper.toImageResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImages(Long productId) {
        return imageRepository.findByProductIdOrderByDisplayOrderAscIdAsc(productId)
                .stream()
                .map(ProductMapper::toImageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductImageResponse setPrimaryImage(Long userId, Long productId, Long imageId) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductImageEntity image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException("Image ID " + imageId + " not found on product ID " + productId));

        imageRepository.resetPrimaryFlags(productId);
        image.setPrimary(true);
        image.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductImageEntity saved = imageRepository.save(image);
        log.info("Set image ID: {} as primary for product ID: {}", imageId, productId);
        return ProductMapper.toImageResponse(saved);
    }

    @Transactional
    public ProductImageResponse updateImageOrder(Long userId, Long productId, Long imageId, Integer displayOrder) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductImageEntity image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException("Image ID " + imageId + " not found on product ID " + productId));

        image.setDisplayOrder(displayOrder != null ? displayOrder : 0);
        image.setUpdatedBy(userRepository.getReferenceById(userId));

        ProductImageEntity saved = imageRepository.save(image);
        log.info("Updated display order to {} for image ID: {} on product ID: {}", displayOrder, imageId, productId);
        return ProductMapper.toImageResponse(saved);
    }

    @Transactional
    public void deleteImage(Long userId, Long productId, Long imageId) {
        productService.getProductAndValidateOwnership(productId, userId);

        ProductImageEntity image = imageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException("Image ID " + imageId + " not found on product ID " + productId));

        boolean wasPrimary = image.isPrimary();
        imageRepository.delete(image);
        log.info("Deleted image ID: {} from product ID: {}", imageId, productId);

        if (wasPrimary) {
            Optional<ProductImageEntity> nextPrimary = imageRepository.findFirstByProductIdOrderByDisplayOrderAscIdAsc(productId);
            if (nextPrimary.isPresent()) {
                ProductImageEntity next = nextPrimary.get();
                next.setPrimary(true);
                next.setUpdatedBy(userRepository.getReferenceById(userId));
                imageRepository.save(next);
                log.info("Promoted image ID: {} as new primary for product ID: {}", next.getId(), productId);
            }
        }
    }
}
