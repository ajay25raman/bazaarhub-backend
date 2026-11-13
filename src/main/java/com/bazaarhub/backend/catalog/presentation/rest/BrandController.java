package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.catalog.application.dto.response.BrandResponse;
import com.bazaarhub.backend.catalog.application.service.BrandService;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Catalog Brands", description = "Public brand exploration and directory APIs")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    @Operation(summary = "List active brands", description = "Public paginated catalog brand directory with optional name search.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<BrandResponse>>> getActiveBrands(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BrandResponse> page = brandService.getBrands(keyword, CatalogStatus.ACTIVE, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all active brands", description = "Retrieves unpaginated alphabetical list of active brands for filters and navigation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active brands retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllActiveBrands() {
        List<BrandResponse> list = brandService.getActiveBrands();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get active brand by ID", description = "Retrieves public details for an active brand. Returns 404 if brand is inactive or missing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found or inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {
        BrandResponse response = brandService.getPublicBrandById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get active brand by slug", description = "Retrieves public details for an active brand using its unique SEO slug.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found or inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandBySlug(@PathVariable String slug) {
        BrandResponse response = brandService.getPublicBrandBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
