package com.company.platform.auth.controller;

import com.company.platform.auth.email.BrevoMailDiagnosticsService;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class MailDiagnosticsControllerTest {

    @Test
    void delegatesDiagnosticsAndSendTest() {
        BrevoMailDiagnosticsService service = mock(BrevoMailDiagnosticsService.class);
        MailDiagnosticsController controller = new MailDiagnosticsController(service);
        given(service.diagnostics()).willReturn(Map.of("status", "diagnostics"));
        given(service.sendTest("to@example.com")).willReturn(Map.of("sent", true));

        assertEquals(controller.brevoDiagnostics().get("status"), "diagnostics");
        assertEquals(controller.sendBrevoTest("to@example.com").get("sent"), true);
    }
}
