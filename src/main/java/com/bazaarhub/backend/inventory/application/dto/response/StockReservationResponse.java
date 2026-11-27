package com.bazaarhub.backend.inventory.application.dto.response;

import com.bazaarhub.backend.inventory.domain.model.ReservationStatus;
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
@Schema(description = "Stock reservation details representation")
public class StockReservationResponse {

    @Schema(description = "Reservation record ID", example = "301")
    private Long id;

    @Schema(description = "Variant ID", example = "101")
    private Long variantId;

    @Schema(description = "Variant SKU", example = "IPHONE15-PROMAX-256-NAT")
    private String sku;

    @Schema(description = "Unique checkout reference", example = "CHK-98234-XYZ")
    private String checkoutReference;

    @Schema(description = "Reserved stock quantity", example = "2")
    private Integer quantity;

    @Schema(description = "Reservation status: ACTIVE, CONFIRMED, RELEASED, EXPIRED, CANCELLED", example = "ACTIVE")
    private ReservationStatus status;

    @Schema(description = "Reservation expiry timestamp", example = "2026-11-13T10:15:00Z")
    private Instant expiresAt;

    @Schema(description = "Creation timestamp", example = "2026-11-13T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2026-11-13T10:00:00Z")
    private Instant updatedAt;
}
