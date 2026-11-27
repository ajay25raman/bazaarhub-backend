package com.bazaarhub.backend.ai.presentation.rest;

import com.bazaarhub.backend.ai.application.dto.request.AiGenerateDescriptionRequest;
import com.bazaarhub.backend.ai.application.dto.response.AiProductDescriptionResponse;
import com.bazaarhub.backend.ai.application.service.AiProductDescriptionService;
import com.bazaarhub.backend.bootstrap.config.OpenApiConfig;
import com.bazaarhub.backend.identity.infrastructure.security.UserPrincipal;
import com.bazaarhub.backend.shared.presentation.rest.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller/ai")
@Tag(name = "Seller AI Catalog Tools", description = "AI-assisted product listing, description writing, and SEO optimization for merchants")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
public class SellerAiCatalogController {

    private final AiProductDescriptionService descriptionService;

    public SellerAiCatalogController(AiProductDescriptionService descriptionService) {
        this.descriptionService = descriptionService;
    }

    @PostMapping("/generate-description")
    @Operation(summary = "Generate AI product titles, descriptions, and SEO tags", description = "Generates high-converting listing drafts in status DRAFT. Output is strictly a draft requiring human merchant approval before publishing.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product draft listing generated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or invalid request parameters", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT Bearer token", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Requires SELLER or ADMIN role", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI service provider error or generation failure", content = @Content)
    })
    public ResponseEntity<ApiResponse<AiProductDescriptionResponse>> generateDescription(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AiGenerateDescriptionRequest request) {

        AiProductDescriptionResponse response = descriptionService.generateContent(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Product draft listing generated successfully", response));
    }
}
