package com.bazaarhub.backend.search.presentation.rest;

import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;
import com.bazaarhub.backend.search.application.service.ProductDiscoveryService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for storefront product discovery channels:
 * Featured, New Arrivals, Related Products, and Category Recommendations.
 */
@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "Product Discovery & Recommendations", description = "Rule-based product discovery channels for home page and product detail cross-selling")
public class ProductDiscoveryController {

    private final ProductDiscoveryService productDiscoveryService;

    public ProductDiscoveryController(ProductDiscoveryService productDiscoveryService) {
        this.productDiscoveryService = productDiscoveryService;
    }

    @GetMapping("/featured")
    @Operation(
            summary = "Get featured products",
            description = "Surfaces top approved active products with active variants. Note: Current schema does not contain an is_featured column; rule-based prioritization by update recency is applied."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Featured products retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> getFeaturedProducts(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSearchResultDto> results = productDiscoveryService.getFeaturedProducts(pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/new-arrivals")
    @Operation(
            summary = "Get new arrival products",
            description = "Returns recently created approved active catalog products sorted by creation timestamp descending."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "New arrivals retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> getNewArrivals(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSearchResultDto> results = productDiscoveryService.getNewArrivals(pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/related/{productId}")
    @Operation(
            summary = "Get related products",
            description = "Recommends products from the same category or brand as the target product, strictly excluding the target product itself."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Related products retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Target product not found or inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> getRelatedProducts(
            @Parameter(description = "Product ID to find related recommendations for", required = true, example = "1")
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSearchResultDto> results = productDiscoveryService.getRelatedProducts(productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/by-category/{categoryId}")
    @Operation(
            summary = "Get recommended products by category",
            description = "Returns active products belonging to the selected category and its active subcategories."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Category recommendations retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category not found or inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> getProductsByCategory(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductSearchResultDto> results = productDiscoveryService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
