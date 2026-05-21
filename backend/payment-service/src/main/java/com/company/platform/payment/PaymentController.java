package com.company.platform.payment;

import com.company.platform.payment.dto.PaymentConfirmationDto;
import com.company.platform.payment.dto.PaymentDto;
import com.company.platform.payment.dto.PaymentRequestDto;
import com.company.platform.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    PaymentDto initiate(@RequestHeader("X-User-Id") UUID userId,
                        @RequestBody PaymentRequestDto request) throws IOException, InterruptedException {
        return paymentService.initiate(userId, request);
    }

    @GetMapping
    List<PaymentDto> list(@RequestHeader("X-User-Id") UUID userId) {
        return paymentService.list(userId);
    }

    @PostMapping("/{paymentId}/confirm")
    PaymentDto confirm(@RequestHeader("X-User-Id") UUID userId,
                       @PathVariable UUID paymentId,
                       @RequestBody PaymentConfirmationDto request) {
        return paymentService.confirm(userId, paymentId, request);
    }

    @PostMapping("/webhook")
    Map<String, Object> webhook(@RequestHeader(value = "Stripe-Signature", required = false) String signature,
                                @RequestBody String payload) throws IOException {
        return paymentService.processWebhook(signature, payload);
    }
}
