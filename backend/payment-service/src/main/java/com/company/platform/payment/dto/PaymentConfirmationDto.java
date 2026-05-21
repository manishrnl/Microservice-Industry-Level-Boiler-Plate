package com.company.platform.payment.dto;

public record PaymentConfirmationDto(
        String sessionId,
        String status
) {
}
