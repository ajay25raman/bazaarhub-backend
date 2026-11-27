package com.bazaarhub.backend.shipping.infrastructure.provider;

import com.bazaarhub.backend.delivery.infrastructure.persistence.entity.ShipmentEntity;
import com.bazaarhub.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.shipping.application.dto.request.CreateInternalShipmentRequest;
import com.bazaarhub.backend.shipping.domain.model.ShippingProviderType;

/**
 * Extensible SPI for logistics shipping providers.
 * Allows BazaarHub's internal delivery fleet to be used by default,
 * while leaving a pluggable interface for future external courier providers.
 */
public interface ShippingProvider {

    ShippingProviderType getProviderType();

    ShipmentEntity createShipment(SellerOrderEntity sellerOrder, CreateInternalShipmentRequest request, UserEntity actor);

    ShipmentEntity cancelShipment(ShipmentEntity shipment, String reason, UserEntity actor);
}
