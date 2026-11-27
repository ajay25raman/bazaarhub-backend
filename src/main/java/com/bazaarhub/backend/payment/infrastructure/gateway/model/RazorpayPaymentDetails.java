package com.bazaarhub.backend.payment.infrastructure.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPaymentDetails {
    private String id;
    private String orderId;
    private Long amount; // in paise
    private BigDecimal amountDecimal;
    private String currency;
    private String status;
    private String method;
    private boolean captured;
    private String email;
    private String contact;
}
