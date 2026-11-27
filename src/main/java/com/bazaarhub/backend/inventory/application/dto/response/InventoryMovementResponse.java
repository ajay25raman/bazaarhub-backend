package com.bazaarhub.backend.inventory.application.dto.response;

import com.bazaarhub.backend.inventory.domain.model.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Stock movement audit history entry")
public class InventoryMovementResponse {

    @Schema(description = "Movement record ID", example = "501")
    private Long id;

    @Schema(description = "Inventory ID", example = "10")
    private Long inventoryId;

    @Schema(description = "Variant ID", example = "101")
    private Long variantId;

    @Schema(description = "Variant SKU", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Schema(description = "Movement type: STOCK_IN, STOCK_OUT, ADJUSTMENT, RESERVE, RELEASE, RETURN", example = "STOCK_IN")
    private MovementType movementType;

    @Schema(description = "Movement quantity (strictly positive integer)", example = "50")
    private Integer quantity;

    @Schema(description = "Reference category (e.g. CHECKOUT, ORDER, MANUAL_RESTOCK, MANUAL_REMOVAL, ADJUSTMENT)", example = "MANUAL_RESTOCK")
    private String referenceType;

    @Schema(description = "External or transactional reference ID", example = "1001")
    private Long referenceId;

    @Schema(description = "Detailed audit notes or reason", example = "Initial warehouse delivery")
    private String notes;

    @Schema(description = "User ID who performed the operation", example = "3")
    private Long createdBy;

    @Schema(description = "Timestamp when the movement occurred", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;
}
