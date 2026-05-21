package com.company.platform.payment.service;

import com.company.platform.payment.dto.PaymentConfirmationDto;
import com.company.platform.payment.dto.PaymentDto;
import com.company.platform.payment.dto.PaymentRequestDto;
import com.company.platform.payment.entity.Payment;
import com.company.platform.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final String defaultCurrency;
    private final String frontendBaseUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public PaymentService(PaymentRepository paymentRepository,
                          ModelMapper modelMapper,
                          ObjectMapper objectMapper,
                          @Value("${payment.stripe.secret-key:}") String stripeSecretKey,
                          @Value("${payment.stripe.webhook-secret:${stripe.webhook.secret:}}") String stripeWebhookSecret,
                          @Value("${payment.default-currency:usd}") String defaultCurrency,
                          @Value("${payment.frontend-base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.paymentRepository = paymentRepository;
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.stripeSecretKey = stripeSecretKey;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.defaultCurrency = defaultCurrency;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public PaymentDto initiate(UUID userId, PaymentRequestDto request) throws IOException, InterruptedException {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }

        UUID paymentId = UUID.randomUUID();
        String method = Optional.ofNullable(request.method())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .orElse("DEMO");
        String currency = Optional.ofNullable(request.currency())
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .orElse(defaultCurrency);

        if ("STRIPE".equals(method) && stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            return createStripeCheckout(userId, paymentId, request, currency);
        }

        return savePayment(userId,
                paymentId,
                "DEMO",
                "READY",
                request.amount(),
                currency,
                null,
                frontendBaseUrl + "/payments?paymentId=" + paymentId + "&status=demo",
                request.description(),
                "Demo checkout is ready. Add STRIPE_SECRET_KEY to enable hosted Stripe Checkout.");
    }

    public List<PaymentDto> list(UUID userId) {
        return paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public PaymentDto confirm(UUID userId, UUID paymentId, PaymentConfirmationDto request) {
        Payment payment = paymentRepository.findByUserIdAndPaymentId(userId, paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment was not found"));

        String status = "success".equalsIgnoreCase(request.status()) || "paid".equalsIgnoreCase(request.status())
                ? "SUCCEEDED"
                : "CANCELLED";
        payment.setStatus(status);
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            payment.setStripeSessionId(request.sessionId());
        }
        return toDto(paymentRepository.save(payment));
    }

    public Map<String, Object> processWebhook(String signature, String payload) throws IOException {
        verifyStripeSignature(signature, payload);
        Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {
        });
        updateFromStripeEvent(event);
        return Map.of(
                "received", true,
                "provider", "STRIPE",
                "eventId", event.getOrDefault("id", ""),
                "eventType", event.getOrDefault("type", "")
        );
    }

    private PaymentDto createStripeCheckout(UUID userId, UUID paymentId, PaymentRequestDto request, String currency) throws IOException, InterruptedException {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", frontendBaseUrl + "/payments?paymentId=" + paymentId + "&status=success&session_id={CHECKOUT_SESSION_ID}");
        form.put("cancel_url", frontendBaseUrl + "/payments?paymentId=" + paymentId + "&status=cancelled");
        form.put("line_items[0][price_data][currency]", currency);
        form.put("line_items[0][price_data][product_data][name]", Optional.ofNullable(request.description()).filter(value -> !value.isBlank()).orElse("Platform payment"));
        form.put("line_items[0][price_data][unit_amount]", request.amount().multiply(BigDecimal.valueOf(100)).setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString());
        form.put("line_items[0][quantity]", "1");
        form.put("metadata[payment_id]", paymentId.toString());

        HttpRequest stripeRequest = HttpRequest.newBuilder(URI.create("https://api.stripe.com/v1/checkout/sessions"))
                .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((stripeSecretKey + ":").getBytes(StandardCharsets.UTF_8)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build();

        HttpResponse<String> response = httpClient.send(stripeRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return savePayment(userId,
                    paymentId,
                    "STRIPE",
                    "FAILED",
                    request.amount(),
                    currency,
                    null,
                    null,
                    request.description(),
                    "Stripe checkout could not be created");
        }

        Map<String, Object> stripe = objectMapper.readValue(response.body(), new TypeReference<>() {
        });
        String checkoutUrl = Optional.ofNullable(stripe.get("url")).map(Object::toString).orElse("");
        String stripeSessionId = Optional.ofNullable(stripe.get("id")).map(Object::toString).orElse("");

        return savePayment(userId,
                paymentId,
                "STRIPE",
                "READY",
                request.amount(),
                currency,
                stripeSessionId,
                checkoutUrl,
                request.description(),
                null);
    }

    private PaymentDto savePayment(UUID userId,
                                   UUID paymentId,
                                   String provider,
                                   String status,
                                   BigDecimal amount,
                                   String currency,
                                   String stripeSessionId,
                                   String checkoutUrl,
                                   String description,
                                   String message) {
        Payment payment = Payment.builder()
                .userId(userId)
                .paymentId(paymentId)
                .provider(provider)
                .status(status)
                .amount(amount)
                .currency(currency.toUpperCase(Locale.ROOT))
                .stripeSessionId(stripeSessionId)
                .checkoutUrl(checkoutUrl)
                .description(description)
                .message(message)
                .build();
        return toDto(paymentRepository.save(payment));
    }

    private void updateFromStripeEvent(Map<String, Object> event) {
        if (!"checkout.session.completed".equals(event.get("type"))) {
            return;
        }

        Object dataObject = event.get("data");
        if (!(dataObject instanceof Map<?, ?> data)) {
            return;
        }

        Object sessionObject = data.get("object");
        if (!(sessionObject instanceof Map<?, ?> session)) {
            return;
        }

        String stripeSessionId = Optional.ofNullable(session.get("id")).map(Object::toString).orElse("");
        Object metadataObject = session.get("metadata");
        if (!(metadataObject instanceof Map<?, ?> metadata)) {
            return;
        }

        String paymentId = Optional.ofNullable(metadata.get("payment_id")).map(Object::toString).orElse("");
        if (paymentId.isBlank()) {
            return;
        }

        paymentRepository.findByPaymentId(UUID.fromString(paymentId))
                .ifPresent(payment -> {
                    payment.setStatus("SUCCEEDED");
                    if (!stripeSessionId.isBlank()) {
                        payment.setStripeSessionId(stripeSessionId);
                    }
                    paymentRepository.save(payment);
                });
    }

    private void verifyStripeSignature(String signature, String payload) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook secret is not configured");
        }
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Stripe signature");
        }

        Map<String, String> parts = java.util.Arrays.stream(signature.split(","))
                .map(part -> part.split("=", 2))
                .filter(part -> part.length == 2)
                .collect(Collectors.toMap(part -> part[0], part -> part[1], (left, right) -> right));

        String timestamp = parts.get("t");
        String expected = parts.get("v1");
        if (timestamp == null || expected == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        }

        long signedAt;
        try {
            signedAt = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature timestamp");
        }
        if (Math.abs(Instant.now().getEpochSecond() - signedAt) > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expired Stripe signature");
        }

        String actual = hmacSha256(timestamp + "." + payload, stripeWebhookSecret);
        if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature");
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stripe signature verification failed");
        }
    }

    private String formBody(Map<String, String> form) {
        return form.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private PaymentDto toDto(Payment payment) {
        return modelMapper.map(payment, PaymentDto.class);
    }
}
