package com.company.platform.auth.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

class BrevoMailDiagnosticsServiceTest {

    @Test
    void diagnosticsReportsConfigurationShapeWithoutNetworkWhenCredentialsAreMissing() {
        BrevoMailDiagnosticsService service = new BrevoMailDiagnosticsService(
                new ObjectMapper(),
                "brevo",
                "no-reply@example.com",
                "Platform",
                "xsmtpsib-not-rest",
                "",
                587,
                "",
                "",
                true,
                true,
                true,
                false
        );

        Map<String, Object> result = service.diagnostics();

        Map<?, ?> configuration = (Map<?, ?>) result.get("configuration");
        Map<?, ?> brevoAccount = (Map<?, ?>) result.get("brevoAccount");
        Map<?, ?> smtpConnection = (Map<?, ?>) result.get("smtpConnection");
        assertEquals(configuration.get("provider"), "brevo");
        assertEquals(configuration.get("brevoApiKeyKind"), "smtp-key");
        assertFalse(((List<?>) result.get("recommendations")).isEmpty());
        assertTrue(String.valueOf(brevoAccount.get("skippedReason").toString()).contains(String.valueOf("REST API key")));
        assertTrue(String.valueOf(smtpConnection.get("skippedReason").toString()).contains(String.valueOf("MAIL_HOST")));
    }
}
