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
@Schema(description = "Brand status transition request payload")
public class BrandStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "Target brand status (ACTIVE or INACTIVE)", example = "ACTIVE")
    private CatalogStatus status;
}
