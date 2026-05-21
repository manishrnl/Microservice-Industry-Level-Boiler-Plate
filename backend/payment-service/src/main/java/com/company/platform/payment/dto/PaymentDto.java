package com.company.platform.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDto(
        UUID paymentId,
        String provider,
        String status,
        BigDecimal amount,
        String currency,
        String stripeSessionId,
        String checkoutUrl,
        String description,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
