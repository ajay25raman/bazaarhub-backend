package com.bazaarhub.backend.catalog.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Brand update request payload")
public class BrandUpdateRequest {

    @NotBlank(message = "Brand name is required")
    @Size(min = 2, max = 150, message = "Brand name must be between 2 and 150 characters")
    @Schema(description = "Official brand or manufacturer name", example = "Apple Inc.")
    private String name;

    @Size(max = 180, message = "Slug must not exceed 180 characters")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase alphanumeric characters and hyphens")
    @Schema(description = "Unique URL slug (optional; preserves current slug or regenerates if blank)", example = "apple-inc")
    private String slug;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    @Schema(description = "Brand official logo image URL", example = "https://cdn.bazaarhub.com/brands/apple-new-logo.png")
    private String logoUrl;
}
