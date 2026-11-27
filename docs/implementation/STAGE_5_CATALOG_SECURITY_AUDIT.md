# BazaarHub — Stage 5 Seller Catalog Security Audit (REM-SEC-04)

> **Document Type**: Security Audit — Seller Catalog Broken Object Level Authorization (BOLA / IDOR)  
> **Target Controllers**: `SellerProductVariantController.java`, `SellerProductImageController.java`  
> **Target Domain**: Merchant Product Catalog Management  
> **Standard**: OWASP API1:2023 — Broken Object Level Authorization  
> **Audit Status**: Complete  

---

## 1. Executive Summary

A comprehensive code-level audit was conducted across all endpoints in `SellerProductVariantController` and `SellerProductImageController`. 

### Key Findings
1. While write operations (`POST`, `PUT`, `PATCH`, `DELETE`) consistently inject `UserPrincipal` and invoke `productService.getProductAndValidateOwnership(productId, userId)`, **read operations were omitting principal injection and ownership validation entirely**.
2. Specifically, three endpoints:
   - `GET /api/v1/seller/products/{productId}/variants`
   - `GET /api/v1/seller/products/{productId}/variants/{variantId}`
   - `GET /api/v1/seller/products/{productId}/images`
   accepted arbitrary `{productId}` path variables and directly queried the database repositories (`findByProductId`, `findByIdAndProductId`) without verifying that the requesting merchant owns the parent product.
3. This allowed any authenticated seller (or user with `ROLE_SELLER` or `PRODUCT_UPDATE`) to read competitor cost structures, selling prices, MRPs, SKU codes, barcodes, attribute configurations, and private/draft photo assets simply by substituting a competitor's `productId`.

---

## 2. Complete Endpoint Inventory & Vulnerability Matrix

| # | HTTP Method | Endpoint Route | Controller & Method | Current Ownership Check | Vulnerability Status | Required Remediation | Expected HTTP Status |
| :-: | :---: | :--- | :--- | :--- | :---: | :--- | :---: |
| 1 | `POST` | `/api/v1/seller/products/{productId}/variants` | `SellerProductVariantController.createVariant` | `variantService.createVariant(principal.getId(), productId, request)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 201 Created / 403 Forbidden |
| 2 | `GET` | `/api/v1/seller/products/{productId}/variants` | `SellerProductVariantController.getVariants` | None (`variantService.getVariants(productId)`) | **CRITICAL BOLA** | Inject `@AuthenticationPrincipal UserPrincipal principal`. Call `getProductAndValidateOwnership(productId, userId, isAdmin)`. | 200 OK / 403 Forbidden / 404 Not Found |
| 3 | `GET` | `/api/v1/seller/products/{productId}/variants/{variantId}` | `SellerProductVariantController.getVariant` | None (`variantService.getVariant(productId, variantId)`) | **CRITICAL BOLA** | Inject `@AuthenticationPrincipal UserPrincipal principal`. Call `getProductAndValidateOwnership(productId, userId, isAdmin)`. | 200 OK / 403 Forbidden / 404 Not Found |
| 4 | `PUT` | `/api/v1/seller/products/{productId}/variants/{variantId}` | `SellerProductVariantController.updateVariant` | `variantService.updateVariant(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |
| 5 | `PATCH` | `/api/v1/seller/products/{productId}/variants/{variantId}/status` | `SellerProductVariantController.updateVariantStatus` | `variantService.updateVariantStatus(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |
| 6 | `DELETE` | `/api/v1/seller/products/{productId}/variants/{variantId}` | `SellerProductVariantController.deleteVariant` | `variantService.deleteVariant(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |
| 7 | `POST` | `/api/v1/seller/products/{productId}/images` | `SellerProductImageController.addImage` | `imageService.addImage(principal.getId(), productId, request)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 201 Created / 403 Forbidden |
| 8 | `GET` | `/api/v1/seller/products/{productId}/images` | `SellerProductImageController.getImages` | None (`imageService.getImages(productId)`) | **CRITICAL BOLA** | Inject `@AuthenticationPrincipal UserPrincipal principal`. Call `getProductAndValidateOwnership(productId, userId, isAdmin)`. | 200 OK / 403 Forbidden / 404 Not Found |
| 9 | `PATCH` | `/api/v1/seller/products/{productId}/images/{imageId}/primary` | `SellerProductImageController.setPrimaryImage` | `imageService.setPrimaryImage(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |
| 10 | `PATCH` | `/api/v1/seller/products/{productId}/images/{imageId}/order` | `SellerProductImageController.updateImageOrder` | `imageService.updateImageOrder(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |
| 11 | `DELETE` | `/api/v1/seller/products/{productId}/images/{imageId}` | `SellerProductImageController.deleteImage` | `imageService.deleteImage(principal.getId(), ...)` | **SECURE** | Already validates ownership via `getProductAndValidateOwnership`. | 200 OK / 403 Forbidden |

---

## 3. Remediation Strategy

1. **Service Layer Overloads**:
   - `ProductService`: Add `getProductAndValidateOwnership(Long productId, Long userId, boolean isAdmin)`. If `isAdmin == true`, bypass seller ownership check while confirming product exists.
   - `ProductVariantService`:
     - Update `getVariants(Long userId, Long productId, boolean isAdmin)`: Calls `productService.getProductAndValidateOwnership(productId, userId, isAdmin)` before fetching variants.
     - Update `getVariant(Long userId, Long productId, Long variantId, boolean isAdmin)`: Calls `productService.getProductAndValidateOwnership(productId, userId, isAdmin)` before fetching variant.
   - `ProductImageService`:
     - Update `getImages(Long userId, Long productId, boolean isAdmin)`: Calls `productService.getProductAndValidateOwnership(productId, userId, isAdmin)` before fetching gallery images.
2. **Controller Layer**:
   - In `SellerProductVariantController`:
     - Inject `@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal` on `getVariants` and `getVariant`.
     - Derive `boolean isAdmin = principal != null && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));`
     - Pass `principal.getId()` and `isAdmin`.
   - In `SellerProductImageController`:
     - Inject `@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal` on `getImages`.
     - Pass `principal.getId()` and `isAdmin`.
3. **Public Storefront Safety**:
   - Confirm that public customer-facing catalog endpoints (`ProductController.java`: `GET /api/v1/products/{id}`, `GET /api/v1/products/slug/{slug}`) remain public and unhindered for approved products.
