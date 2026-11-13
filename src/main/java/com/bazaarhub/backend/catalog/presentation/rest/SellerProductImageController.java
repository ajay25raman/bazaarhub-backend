package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.catalog.application.dto.request.ProductImageOrderRequest;
import com.bazaarhub.backend.catalog.application.dto.request.ProductImageRequest;
import com.bazaarhub.backend.catalog.application.dto.response.ProductImageResponse;
import com.bazaarhub.backend.catalog.application.service.ProductImageService;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seller/products/{productId}/images")
@Tag(name = "Seller Product Images", description = "Endpoints for managing product photo gallery metadata")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyAuthority('PRODUCT_UPDATE', 'ROLE_SELLER')")
public class SellerProductImageController {

    private final ProductImageService imageService;

    public SellerProductImageController(ProductImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    @Operation(summary = "Add image to product", description = "Associates an image URL with the product. If isPrimary is true, existing primary is demoted.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Image added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid image payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductImageResponse>> addImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductImageResponse response = imageService.addImage(principal.getId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Image added successfully", response));
    }

    @GetMapping
    @Operation(summary = "List product images", description = "Retrieves all images associated with the product in display order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Images retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getImages(@PathVariable Long productId) {
        List<ProductImageResponse> images = imageService.getImages(productId);
        return ResponseEntity.ok(ApiResponse.ok(images));
    }

    @PatchMapping("/{imageId}/primary")
    @Operation(summary = "Set primary hero image", description = "Marks this image as the primary hero image and unsets all other primaries.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Primary image updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductImageResponse>> setPrimaryImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductImageResponse response = imageService.setPrimaryImage(principal.getId(), productId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Primary image updated successfully", response));
    }

    @PatchMapping("/{imageId}/order")
    @Operation(summary = "Update image display order", description = "Updates the display order sorting index of a product image.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Display order updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateImageOrder(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Valid @RequestBody ProductImageOrderRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        ProductImageResponse response = imageService.updateImageOrder(principal.getId(), productId, imageId, request.getDisplayOrder());
        return ResponseEntity.ok(ApiResponse.ok("Display order updated successfully", response));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete product image", description = "Removes an image. If it was primary, the next available image is automatically promoted.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Image deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Not product owner", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Image not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        imageService.deleteImage(principal.getId(), productId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Image deleted successfully", null));
    }
}
