package com.bazaarhub.backend.catalog.application.dto.request;

import com.bazaarhub.backend.catalog.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product variant status toggle request")
public class ProductVariantStatusRequest {

    @NotNull(message = "Variant status is required")
    @Schema(description = "Target variant status (ACTIVE or INACTIVE)", example = "ACTIVE")
    private CatalogStatus status;
}
