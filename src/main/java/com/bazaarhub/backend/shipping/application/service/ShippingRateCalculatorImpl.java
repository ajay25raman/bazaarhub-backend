package com.bazaarhub.backend.shipping.application.service;

import com.bazaarhub.backend.delivery.infrastructure.adapter.GoogleMapsService;
import com.bazaarhub.backend.order.domain.exception.SellerOrderNotFoundException;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.OrderItemEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.entity.SellerOrderEntity;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
import com.bazaarhub.backend.shipping.domain.model.ShippingRateQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ShippingRateCalculatorImpl implements ShippingRateCalculator {

    private static final Logger log = LoggerFactory.getLogger(ShippingRateCalculatorImpl.class);

    private final SpringDataSellerOrderRepository sellerOrderRepository;
    private final GoogleMapsService googleMapsService;

    private final BigDecimal baseRate;
    private final BigDecimal freeShippingThreshold;
    private final BigDecimal ratePerKm;
    private final BigDecimal baseDistanceKm;
    private final BigDecimal ratePerKg;
    private final BigDecimal baseWeightKg;
    private final BigDecimal maxShippingFee;

    public ShippingRateCalculatorImpl(
            SpringDataSellerOrderRepository sellerOrderRepository,
            GoogleMapsService googleMapsService,
            @Value("${bazaarhub.shipping.base-rate:40.00}") BigDecimal baseRate,
            @Value("${bazaarhub.shipping.free-threshold:500.00}") BigDecimal freeShippingThreshold,
            @Value("${bazaarhub.shipping.rate-per-km:5.00}") BigDecimal ratePerKm,
            @Value("${bazaarhub.shipping.base-distance-km:5.0}") BigDecimal baseDistanceKm,
            @Value("${bazaarhub.shipping.rate-per-kg:10.00}") BigDecimal ratePerKg,
            @Value("${bazaarhub.shipping.base-weight-kg:1.0}") BigDecimal baseWeightKg,
            @Value("${bazaarhub.shipping.max-fee:200.00}") BigDecimal maxShippingFee) {
        this.sellerOrderRepository = sellerOrderRepository;
        this.googleMapsService = googleMapsService;
        this.baseRate = baseRate;
        this.freeShippingThreshold = freeShippingThreshold;
        this.ratePerKm = ratePerKm;
        this.baseDistanceKm = baseDistanceKm;
        this.ratePerKg = ratePerKg;
        this.baseWeightKg = baseWeightKg;
        this.maxShippingFee = maxShippingFee;
    }

    @Override
    public ShippingRateQuote calculateRate(ShippingRateQuoteRequest request) {
        if (request == null) {
            request = ShippingRateQuoteRequest.builder().build();
        }

        BigDecimal weight = request.getWeightKg() != null ? request.getWeightKg() : BigDecimal.valueOf(0.5);
        BigDecimal subtotal = request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO;

        BigDecimal distanceKm = BigDecimal.valueOf(5.0); // Default local base distance
        if (request.getOriginCity() != null && request.getDestinationCity() != null) {
            if (!request.getOriginCity().equalsIgnoreCase(request.getDestinationCity())) {
                distanceKm = BigDecimal.valueOf(25.0); // Inter-city within region estimate
            } else {
                distanceKm = BigDecimal.valueOf(7.5);
            }
        }

        return computeBreakdown(distanceKm, weight, subtotal);
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingRateQuote calculateRateForSellerOrder(Long sellerOrderId) {
        SellerOrderEntity sellerOrder = sellerOrderRepository.findById(sellerOrderId)
                .orElseThrow(() -> new SellerOrderNotFoundException(sellerOrderId));

        // 1. Calculate weight from items
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (sellerOrder.getItems() != null) {
            for (OrderItemEntity item : sellerOrder.getItems()) {
                BigDecimal itemWeight = (item.getVariant() != null && item.getVariant().getWeight() != null)
                        ? item.getVariant().getWeight()
                        : BigDecimal.valueOf(0.5);
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                totalWeight = totalWeight.add(itemWeight.multiply(BigDecimal.valueOf(qty)));
            }
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            totalWeight = BigDecimal.valueOf(0.5);
        }

        // 2. Estimate route distance via Google Maps Service or fallback
        BigDecimal destLat = null;
        BigDecimal destLng = null;
        String destAddress = null;
        if (sellerOrder.getOrder() != null) {
            if (sellerOrder.getOrder().getShippingAddress() != null) {
                destLat = sellerOrder.getOrder().getShippingAddress().getLatitude();
                destLng = sellerOrder.getOrder().getShippingAddress().getLongitude();
            }
            destAddress = sellerOrder.getOrder().getAddressLine1() + ", " + sellerOrder.getOrder().getCity();
        }

        BigDecimal originLat = new BigDecimal("12.9716000");
        BigDecimal originLng = new BigDecimal("77.5946000");

        GoogleMapsService.RouteDetails route = googleMapsService.calculateRoute(
                originLat, originLng, destLat, destLng, "BazaarHub Hub", destAddress);

        BigDecimal distanceKm = route.distanceKm() != null ? route.distanceKm() : BigDecimal.valueOf(5.0);

        return computeBreakdown(distanceKm, totalWeight, sellerOrder.getSubtotal());
    }

    private ShippingRateQuote computeBreakdown(BigDecimal distanceKm, BigDecimal weightKg, BigDecimal subtotal) {
        BigDecimal base = this.baseRate.setScale(2, RoundingMode.HALF_UP);

        // Distance surcharge beyond base distance
        BigDecimal extraDistance = distanceKm.subtract(this.baseDistanceKm);
        BigDecimal distanceCharge = BigDecimal.ZERO;
        if (extraDistance.compareTo(BigDecimal.ZERO) > 0) {
            distanceCharge = extraDistance.multiply(this.ratePerKm).setScale(2, RoundingMode.HALF_UP);
        }

        // Weight surcharge beyond base weight
        BigDecimal extraWeight = weightKg.subtract(this.baseWeightKg);
        BigDecimal weightCharge = BigDecimal.ZERO;
        if (extraWeight.compareTo(BigDecimal.ZERO) > 0) {
            weightCharge = extraWeight.multiply(this.ratePerKg).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal rawTotal = base.add(distanceCharge).add(weightCharge);
        if (rawTotal.compareTo(this.maxShippingFee) > 0) {
            rawTotal = this.maxShippingFee;
        }

        boolean freeShipping = subtotal != null && subtotal.compareTo(this.freeShippingThreshold) >= 0;
        BigDecimal finalCharge = freeShipping ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : rawTotal;

        log.debug("[SHIPPING RATE] Base: {}, Dist: {}km (fee: {}), Weight: {}kg (fee: {}), Subtotal: {}, Final: {}",
                base, distanceKm, distanceCharge, weightKg, weightCharge, subtotal, finalCharge);

        return ShippingRateQuote.builder()
                .baseCharge(base)
                .distanceKm(distanceKm.setScale(2, RoundingMode.HALF_UP))
                .distanceCharge(distanceCharge)
                .weightKg(weightKg.setScale(3, RoundingMode.HALF_UP))
                .weightCharge(weightCharge)
                .freeShippingApplied(freeShipping)
                .freeShippingThreshold(this.freeShippingThreshold.setScale(2, RoundingMode.HALF_UP))
                .totalShippingCharge(finalCharge)
                .currency("INR")
                .calculationMethod("INTERNAL_DISTANCE_WEIGHT_MODEL")
                .build();
    }
}
