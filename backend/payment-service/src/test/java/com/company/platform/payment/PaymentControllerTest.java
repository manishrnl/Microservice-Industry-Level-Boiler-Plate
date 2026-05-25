package com.company.platform.payment;

import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.payment.dto.PaymentConfirmationDto;
import com.company.platform.payment.dto.PaymentDto;
import com.company.platform.payment.dto.PaymentRequestDto;
import com.company.platform.payment.service.PaymentService;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class PaymentControllerTest {
    private final PaymentService service = mock(PaymentService.class);
    private final PaymentController controller = new PaymentController(service);

    @Test
    void delegatesPaymentEndpointsToService() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentRequestDto request = new PaymentRequestDto(new BigDecimal("10.00"), "INR", "DEMO", "Test");
        PaymentConfirmationDto confirmation = new PaymentConfirmationDto("session-1", "success");
        PaymentDto dto = dto(paymentId, "READY");
        DemoUserRequestDto demo = new DemoUserRequestDto(userId, "u@example.com", "User", "user", null);
        given(service.initiate(userId, request)).willReturn(dto);
        given(service.list(userId)).willReturn(List.of(dto));
        given(service.confirm(userId, paymentId, confirmation)).willReturn(dto(paymentId, "SUCCEEDED"));
        given(service.processWebhook("sig", "{}")).willReturn(Map.of("received", true));
        given(service.seedDemoData(demo)).willReturn(List.of(dto));

        assertEquals(controller.initiate(userId, request), dto);
        assertEquals(controller.list(userId), List.of(dto));
        assertEquals(controller.confirm(userId, paymentId, confirmation).status(), "SUCCEEDED");
        assertEquals(controller.webhook("sig", "{}").get("received"), true);
        assertEquals(controller.seedDemoData(demo), List.of(dto));
    }

    private PaymentDto dto(UUID paymentId, String status) {
        return new PaymentDto(paymentId, "DEMO", status, new BigDecimal("10.00"), "INR",
                null, "/payments", "Test", "Ready", LocalDateTime.now(), LocalDateTime.now());
    }
}
