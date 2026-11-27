package com.bazaarhub.backend.shipping;

import com.bazaarhub.backend.delivery.infrastructure.adapter.GoogleMapsService;
import com.bazaarhub.backend.order.infrastructure.persistence.repository.SpringDataSellerOrderRepository;
import com.bazaarhub.backend.shipping.application.dto.request.ShippingRateQuoteRequest;
import com.bazaarhub.backend.shipping.application.service.ShippingRateCalculator;
import com.bazaarhub.backend.shipping.application.service.ShippingRateCalculatorImpl;
import com.bazaarhub.backend.shipping.domain.model.ShippingRateQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ShippingRateCalculatorUnitTest {

    @Mock
    private SpringDataSellerOrderRepository sellerOrderRepository;

    @Mock
    private GoogleMapsService googleMapsService;

    private ShippingRateCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShippingRateCalculatorImpl(
                sellerOrderRepository,
                googleMapsService,
                new BigDecimal("40.00"),  // base rate
                new BigDecimal("500.00"), // free shipping threshold
                new BigDecimal("5.00"),   // rate per km
                new BigDecimal("5.0"),    // base distance km
                new BigDecimal("10.00"),  // rate per kg
                new BigDecimal("1.0"),    // base weight kg
                new BigDecimal("200.00")  // max fee
        );
    }

    @Test
    @DisplayName("Calculate rate: Standard local parcel under weight threshold with subtotal under 500")
    void testStandardLocalParcel() {
        ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                .originCity("Bengaluru")
                .destinationCity("Bengaluru")
                .weightKg(new BigDecimal("0.800"))
                .subtotal(new BigDecimal("250.00"))
                .build();

        ShippingRateQuote quote = calculator.calculateRate(request);

        assertNotNull(quote);
        assertEquals(new BigDecimal("40.00"), quote.getBaseCharge());
        assertFalse(quote.isFreeShippingApplied());
        // 7.5km distance - 5.0km base = 2.5km * 5.00 = 12.50 distance charge
        assertEquals(new BigDecimal("12.50"), quote.getDistanceCharge());
        // 0.800kg < 1.0kg base = 0.00 weight charge
        assertEquals(BigDecimal.ZERO, quote.getWeightCharge());
        // Total = 40.00 + 12.50 = 52.50
        assertEquals(new BigDecimal("52.50"), quote.getTotalShippingCharge());
    }

    @Test
    @DisplayName("Calculate rate: Free shipping applied when subtotal >= 500")
    void testFreeShippingThreshold() {
        ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                .originCity("Bengaluru")
                .destinationCity("Bengaluru")
                .weightKg(new BigDecimal("2.500"))
                .subtotal(new BigDecimal("750.00"))
                .build();

        ShippingRateQuote quote = calculator.calculateRate(request);

        assertNotNull(quote);
        assertTrue(quote.isFreeShippingApplied());
        assertEquals(new BigDecimal("0.00"), quote.getTotalShippingCharge());
    }

    @Test
    @DisplayName("Calculate rate: Heavy parcel with extra weight surcharge")
    void testHeavyParcelWeightSurcharge() {
        ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                .originCity("Bengaluru")
                .destinationCity("Bengaluru")
                .weightKg(new BigDecimal("3.000")) // 2.0kg over base = 2.0 * 10.00 = 20.00
                .subtotal(new BigDecimal("300.00"))
                .build();

        ShippingRateQuote quote = calculator.calculateRate(request);

        assertNotNull(quote);
        assertFalse(quote.isFreeShippingApplied());
        assertEquals(new BigDecimal("20.00"), quote.getWeightCharge());
        // Base 40.00 + Dist 12.50 + Weight 20.00 = 72.50
        assertEquals(new BigDecimal("72.50"), quote.getTotalShippingCharge());
    }

    @Test
    @DisplayName("Calculate rate: Long distance capped at maximum fee")
    void testMaxFeeCap() {
        ShippingRateQuoteRequest request = ShippingRateQuoteRequest.builder()
                .originCity("Bengaluru")
                .destinationCity("Mysuru") // 25km estimated distance
                .weightKg(new BigDecimal("20.000")) // heavy weight
                .subtotal(new BigDecimal("100.00"))
                .build();

        ShippingRateQuote quote = calculator.calculateRate(request);

        assertNotNull(quote);
        assertEquals(new BigDecimal("200.00"), quote.getTotalShippingCharge());
    }
}
