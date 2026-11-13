package com.bazaarhub.backend.catalog.application.dto.request;

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
@Schema(description = "Product image display order reordering request")
public class ProductImageOrderRequest {

    @NotNull(message = "Display order is required")
    @Schema(description = "New display sorting order index", example = "1")
    private Integer displayOrder;
}
