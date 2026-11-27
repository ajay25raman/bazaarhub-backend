package com.bazaarhub.backend.search.presentation.rest;

import com.bazaarhub.backend.search.application.dto.request.ProductSearchRequest;
import com.bazaarhub.backend.search.application.dto.response.ProductSearchResultDto;
import com.bazaarhub.backend.search.application.service.ProductSearchService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public REST controller for storefront product search, faceted filtering,
 * and category-based product discovery.
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Product Search & Catalog Discovery", description = "Public endpoints for searching products, filtering by brand/category/price/stock, and browsing categories")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products")
    @Operation(
            summary = "Search public catalog products",
            description = "Multi-criteria search over active products and variants. Supports keyword search (name, description, SKU), category/subcategory filtering, brand and seller filtering, price range, in-stock availability, safe sorting, and pagination."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Search results retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter or pagination parameters (e.g., negative prices, minPrice > maxPrice, page size > 100)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> searchProducts(
            @Valid @ModelAttribute ProductSearchRequest request) {
        Page<ProductSearchResultDto> results = productSearchService.searchProducts(request);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/categories/{categoryId}/products")
    @Operation(
            summary = "Browse products by category",
            description = "Category-based product discovery including active subcategories. Supports combining with keyword, price range, stock, brand, seller filters, and sorting."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Category products retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter or pagination parameters",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category not found or inactive",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<Page<ProductSearchResultDto>>> searchCategoryProducts(
            @Parameter(description = "Category ID to browse", required = true, example = "1")
            @PathVariable Long categoryId,
            @Valid @ModelAttribute ProductSearchRequest request) {
        Page<ProductSearchResultDto> results = productSearchService.searchCategoryProducts(categoryId, request);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
