package com.bazaarhub.backend.inventory.presentation.rest;

import com.bazaarhub.backend.inventory.application.dto.response.PublicStockAvailabilityResponse;
import com.bazaarhub.backend.inventory.application.service.PublicInventoryService;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/variants/{variantId}/availability")
@Tag(name = "Public Product Availability", description = "Public endpoints for checking product SKU purchasability and in-stock/low-stock status without exposing internal quantities")
public class PublicStockAvailabilityController {

    private final PublicInventoryService publicInventoryService;

    public PublicStockAvailabilityController(PublicInventoryService publicInventoryService) {
        this.publicInventoryService = publicInventoryService;
    }

    @GetMapping
    @Operation(summary = "Check public stock availability", description = "Returns minimal stock status (IN_STOCK, LOW_STOCK, OUT_OF_STOCK) and purchasability flag for approved and active catalog products.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Availability checked successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product or variant not found or inactive", content = @Content)
    })
    public ResponseEntity<ApiResponse<PublicStockAvailabilityResponse>> checkAvailability(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        PublicStockAvailabilityResponse response = publicInventoryService.getPublicVariantAvailability(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
