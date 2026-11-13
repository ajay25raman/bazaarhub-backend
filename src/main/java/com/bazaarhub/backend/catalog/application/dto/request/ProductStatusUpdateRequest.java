package com.bazaarhub.backend.catalog.application.dto.request;

import com.bazaarhub.backend.catalog.domain.model.ProductStatus;
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
@Schema(description = "Product lifecycle status update request")
public class ProductStatusUpdateRequest {

    @NotNull(message = "Product status is required")
    @Schema(description = "Target product lifecycle status (DRAFT, ACTIVE, INACTIVE, ARCHIVED)", example = "ACTIVE")
    private ProductStatus status;
}
