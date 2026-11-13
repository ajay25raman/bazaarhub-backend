package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.catalog.application.dto.response.ProductDetailResponse;
import com.bazaarhub.backend.catalog.application.dto.response.ProductSummaryResponse;
import com.bazaarhub.backend.catalog.application.service.PublicProductService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Catalog", description = "Public customer storefront product browsing and search APIs")
public class ProductController {

    private final PublicProductService publicProductService;

    public ProductController(PublicProductService publicProductService) {
        this.publicProductService = publicProductService;
    }

    @GetMapping
    @Operation(summary = "Search public catalog products", description = "Paginated listing of approved, active products for storefront display with optional category, brand, and keyword filters.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Catalog products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSummaryResponse> page = publicProductService.getPublicProducts(categoryId, brandId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product details by ID", description = "Retrieves public product details, gallery images, and active variants for customer storefront.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found or not published", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable Long productId) {
        ProductDetailResponse response = publicProductService.getPublicProductById(productId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product details by slug", description = "SEO-friendly endpoint retrieving product details by URL slug.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product details retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found or not published", content = @Content)
    })
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String slug) {
        ProductDetailResponse response = publicProductService.getPublicProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
