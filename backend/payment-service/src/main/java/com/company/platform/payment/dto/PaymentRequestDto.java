package com.company.platform.payment.dto;

import java.math.BigDecimal;

public record PaymentRequestDto(
        BigDecimal amount,
        String currency,
        String method,
        String description
) {
}
