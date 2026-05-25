package com.company.platform.payment.service;

import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.payment.dto.PaymentConfirmationDto;
import com.company.platform.payment.dto.PaymentRequestDto;
import com.company.platform.payment.entity.Payment;
import com.company.platform.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class PaymentServiceTest {
    private static final String WEBHOOK_SECRET = "whsec_test";

    @Mock
    private PaymentRepository repository;

    private PaymentService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PaymentService(repository, new ObjectMapper(), "", WEBHOOK_SECRET, "usd", "http://localhost:5173");
        given(repository.save(any(Payment.class))).willAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(LocalDateTime.now());
            }
            if (payment.getUpdatedAt() == null) {
                payment.setUpdatedAt(payment.getCreatedAt());
            }
            return payment;
        });
    }

    @Test
    void initiateRejectsMissingOrNonPositiveAmount() {
        UUID userId = UUID.randomUUID();

        ResponseStatusException missingAmount = expectThrows(
                ResponseStatusException.class,
                () -> service.initiate(userId, new PaymentRequestDto(null, "INR", "DEMO", "Bad"))
        );
        assertEquals(missingAmount.getStatusCode(), HttpStatus.BAD_REQUEST);
        expectThrows(
                ResponseStatusException.class,
                () -> service.initiate(userId, new PaymentRequestDto(BigDecimal.ZERO, "INR", "DEMO", "Bad"))
        );
    }

    @Test
    void initiateCreatesDemoCheckoutWhenStripeSecretIsBlank() throws Exception {
        UUID userId = UUID.randomUUID();

        var dto = service.initiate(userId, new PaymentRequestDto(new BigDecimal("49.50"), "inr", null, "Starter"));

        assertEquals(dto.provider(), "DEMO");
        assertEquals(dto.status(), "READY");
        assertEquals(dto.currency(), "INR");
        assertTrue(String.valueOf(dto.checkoutUrl()).contains(String.valueOf("paymentId=")));
        assertTrue(String.valueOf(dto.checkoutUrl()).contains(String.valueOf("status=demo")));
        assertTrue(String.valueOf(dto.message()).contains(String.valueOf("Demo checkout")));
    }

    @Test
    void initiateCreatesHostedStripeCheckoutWhenSecretIsConfigured() throws Exception {
        HttpClient httpClient = mockStripeClient(200, "{\"id\":\"cs_test\",\"url\":\"https://checkout.example/session\"}");
        PaymentService stripeService = stripeService(httpClient);

        var dto = stripeService.initiate(UUID.randomUUID(),
                new PaymentRequestDto(new BigDecimal("49.50"), "inr", "stripe", "Starter plan"));

        assertEquals(dto.provider(), "STRIPE");
        assertEquals(dto.status(), "READY");
        assertEquals(dto.currency(), "INR");
        assertEquals(dto.stripeSessionId(), "cs_test");
        assertEquals(dto.checkoutUrl(), "https://checkout.example/session");
        verify(httpClient).send(argThat((HttpRequest request) ->
                        request.uri().toString().equals("https://api.stripe.com/v1/checkout/sessions")
                                && request.headers().firstValue("Authorization").orElse("").startsWith("Basic ")
                                && request.headers().firstValue("Content-Type").orElse("").equals("application/x-www-form-urlencoded")),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void initiatePersistsFailedStripePaymentWhenCheckoutApiRejectsRequest() throws Exception {
        PaymentService stripeService = stripeService(mockStripeClient(402, "{\"error\":\"payment required\"}"));

        var dto = stripeService.initiate(UUID.randomUUID(),
                new PaymentRequestDto(new BigDecimal("10.00"), "", "STRIPE", ""));

        assertEquals(dto.provider(), "STRIPE");
        assertEquals(dto.status(), "FAILED");
        assertEquals(dto.currency(), "USD");
        assertNull(dto.checkoutUrl());
        assertEquals(dto.message(), "Stripe checkout could not be created");
    }

    @Test
    void listMapsStoredPayments() {
        UUID userId = UUID.randomUUID();
        Payment payment = payment(userId, UUID.randomUUID(), "READY");
        given(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of(payment));

        var rows = service.list(userId);
        assertEquals(rows.size(), 1);
        assertEquals(rows.getFirst().paymentId(), payment.getPaymentId());
        assertEquals(rows.getFirst().status(), "READY");
    }

    @Test
    void confirmUpdatesPaymentStatusAndSessionId() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(userId, paymentId, "READY");
        given(repository.findByUserIdAndPaymentId(userId, paymentId)).willReturn(Optional.of(payment));

        assertEquals(service.confirm(userId, paymentId, new PaymentConfirmationDto("cs_123", "paid")).status(), "SUCCEEDED");
        assertEquals(payment.getStripeSessionId(), "cs_123");
        assertEquals(service.confirm(userId, paymentId, new PaymentConfirmationDto("", "cancelled")).status(), "CANCELLED");
    }

    @Test
    void confirmThrowsWhenPaymentIsMissing() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        given(repository.findByUserIdAndPaymentId(userId, paymentId)).willReturn(Optional.empty());

        ResponseStatusException exception = expectThrows(
                ResponseStatusException.class,
                () -> service.confirm(userId, paymentId, new PaymentConfirmationDto(null, "paid"))
        );
        assertEquals(exception.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    void seedDemoDataCreatesOnlyMissingDescriptionsThenReturnsList() {
        UUID userId = UUID.randomUUID();
        given(repository.existsByUserIdAndDescription(userId, "Platform starter plan")).willReturn(true);
        given(repository.findAllByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of(payment(userId, UUID.randomUUID(), "READY")));

        assertEquals(service.seedDemoData(new DemoUserRequestDto(userId, "u@example.com", "User", "user", null)).size(), 1);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.argThat(payment -> "Platform starter plan".equals(payment.getDescription())));
        verify(repository, org.mockito.Mockito.times(4)).save(any(Payment.class));
    }

    @Test
    void processWebhookRequiresConfiguredAndValidSignature() {
        PaymentService missingSecret = new PaymentService(repository, new ObjectMapper(), "", "", "usd", "http://localhost:5173");

        ResponseStatusException missingSignature = expectThrows(
                ResponseStatusException.class,
                () -> missingSecret.processWebhook(null, "{}")
        );
        assertEquals(missingSignature.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE);
        ResponseStatusException blankSignature = expectThrows(
                ResponseStatusException.class,
                () -> service.processWebhook("", "{}")
        );
        assertEquals(blankSignature.getStatusCode(), HttpStatus.BAD_REQUEST);
        ResponseStatusException invalidSignature = expectThrows(
                ResponseStatusException.class,
                () -> service.processWebhook("t=abc,v1=nope", "{}")
        );
        assertEquals(invalidSignature.getStatusCode(), HttpStatus.BAD_REQUEST);
        ResponseStatusException missingExpectedSignature = expectThrows(
                ResponseStatusException.class,
                () -> service.processWebhook("t=" + Instant.now().getEpochSecond(), "{}")
        );
        assertEquals(missingExpectedSignature.getStatusCode(), HttpStatus.BAD_REQUEST);
        ResponseStatusException expiredSignature = expectThrows(
                ResponseStatusException.class,
                () -> service.processWebhook("t=" + Instant.now().minusSeconds(301).getEpochSecond() + ",v1=nope", "{}")
        );
        assertEquals(expiredSignature.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @Test
    void processWebhookUpdatesCompletedCheckoutPayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(UUID.randomUUID(), paymentId, "READY");
        String payload = """
                {"id":"evt_1","type":"checkout.session.completed","data":{"object":{"id":"cs_123","metadata":{"payment_id":"%s"}}}}
                """.formatted(paymentId);
        given(repository.findByPaymentId(paymentId)).willReturn(Optional.of(payment));

        var result = service.processWebhook(signature(payload), payload);

        assertEquals(result.get("received"), true);
        assertEquals(result.get("eventId"), "evt_1");
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertEquals(captor.getValue().getStatus(), "SUCCEEDED");
        assertEquals(captor.getValue().getStripeSessionId(), "cs_123");
    }

    @Test
    void processWebhookIgnoresUnsupportedStripeEventsAfterSignaturePasses() throws Exception {
        String payload = "{\"id\":\"evt_2\",\"type\":\"customer.created\"}";

        var result = service.processWebhook(signature(payload), payload);

        assertEquals(result.get("eventType"), "customer.created");
        verify(repository, never()).findByPaymentId(any());
    }

    @Test
    void processWebhookLeavesCompletedCheckoutUntouchedWhenMetadataIsMissing() throws Exception {
        String payload = "{\"id\":\"evt_3\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\"cs_missing\",\"metadata\":{}}}}";

        var result = service.processWebhook(signature(payload), payload);

        assertEquals(result.get("eventType"), "checkout.session.completed");
        verify(repository, never()).findByPaymentId(any());
    }

    private Payment payment(UUID userId, UUID paymentId, String status) {
        return Payment.builder()
                .userId(userId)
                .paymentId(paymentId)
                .provider("DEMO")
                .status(status)
                .amount(new BigDecimal("10.00"))
                .currency("INR")
                .checkoutUrl("/payments")
                .description("Starter")
                .message("Ready")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String signature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        // Stripe signs the exact string "{timestamp}.{rawPayload}" before sending it in the v1 field.
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String digest = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + digest;
    }

    private PaymentService stripeService(HttpClient httpClient) {
        PaymentService stripeService = new PaymentService(repository, new ObjectMapper(), "sk_test", WEBHOOK_SECRET, "usd", "http://localhost:5173");
        ReflectionTestUtils.setField(stripeService, "httpClient", httpClient);
        return stripeService;
    }

    private HttpClient mockStripeClient(int statusCode, String body) throws Exception {
        HttpClient httpClient = org.mockito.Mockito.mock(HttpClient.class);
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        given(response.statusCode()).willReturn(statusCode);
        given(response.body()).willReturn(body);
        given(httpClient.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).willReturn(response);
        return httpClient;
    }
}
