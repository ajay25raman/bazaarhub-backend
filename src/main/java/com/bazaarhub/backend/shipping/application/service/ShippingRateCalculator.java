package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
import com.bazaarhub.backend.shipping.domain.model.ShippingRateQuote;

public interface ShippingRateCalculator {

    ShippingRateQuote calculateRate(ShippingRateQuoteRequest request);

    ShippingRateQuote calculateRateForSellerOrder(Long sellerOrderId);
}
