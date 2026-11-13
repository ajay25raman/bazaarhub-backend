package com.bazaarhub.backend.catalog.presentation.rest;

import com.bazaarhub.backend.catalog.application.dto.response.CategoryResponse;
import com.bazaarhub.backend.catalog.application.dto.response.CategoryTreeResponse;
import com.bazaarhub.backend.catalog.application.service.CategoryService;
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
@RequestMapping("/api/v1/categories")
@Tag(name = "Catalog Categories", description = "Public category exploration and navigation APIs")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List active categories", description = "Public paginated catalog category listing with optional name search and parent filter.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<CategoryResponse>>> getActiveCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long parentId,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<CategoryResponse> page = categoryService.getCategories(keyword, parentId, CatalogStatus.ACTIVE, pageable);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get category hierarchy", description = "Returns the complete hierarchical tree of active categories with nested subcategories.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category tree retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree(true);
        return ResponseEntity.ok(ApiResponse.ok(tree));
    }

    @GetMapping("/root")
    @Operation(summary = "Get root categories", description = "Returns top-level root categories that have no parent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Root categories retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> roots = categoryService.getRootCategories(true);
        return ResponseEntity.ok(ApiResponse.ok(roots));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get active category by ID", description = "Retrieves public details for an active category. Returns 404 if category is inactive or missing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found or inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getPublicCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get active category by slug", description = "Retrieves public details for an active category using its unique SEO slug.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found or inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse response = categoryService.getPublicCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
