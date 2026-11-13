package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.catalog.application.dto.request.BrandCreateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.BrandStatusUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.request.BrandUpdateRequest;
import com.bazaarhub.backend.catalog.application.dto.response.BrandResponse;
import com.bazaarhub.backend.catalog.application.service.BrandService;
import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/brands")
@Tag(name = "Admin Brand Management", description = "Administrative taxonomy APIs for brand and manufacturer management")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyAuthority('BRAND_MANAGE', 'ROLE_ADMIN')")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @PostMapping
    @Operation(summary = "Create brand", description = "Creates a new catalog brand. Only accessible by platform administrators.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Brand created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin authority required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict - Brand name or slug already in use", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandCreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long adminUserId = principal != null ? principal.getId() : null;
        BrandResponse response = brandService.createBrand(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Brand created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List brands", description = "Paginated brand retrieval with optional keyword and status filters.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<BrandResponse>>> getBrands(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CatalogStatus status,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<BrandResponse> page = brandService.getBrands(keyword, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get brand by ID", description = "Retrieves brand details by ID (including inactive brands).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update brand", description = "Updates brand details, name, slug, and logo.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Brand name or slug already in use", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long adminUserId = principal != null ? principal.getId() : null;
        BrandResponse response = brandService.updateBrand(id, request, adminUserId);
        return ResponseEntity.ok(ApiResponse.ok("Brand updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate brand", description = "Transitions brand status. Prevents deactivation when active products are linked.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brand status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Brand not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Operation not allowed due to linked active products", content = @Content)
    })
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrandStatus(
            @PathVariable Long id,
            @Valid @RequestBody BrandStatusUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        Long adminUserId = principal != null ? principal.getId() : null;
        BrandResponse response = brandService.updateBrandStatus(id, request.getStatus(), adminUserId);
        return ResponseEntity.ok(ApiResponse.ok("Brand status updated successfully", response));
    }
}
