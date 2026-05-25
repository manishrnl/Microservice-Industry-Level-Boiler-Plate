package com.company.platform.auth.email;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

class AuthEmailTemplatesTest {
    private final AuthEmailTemplates templates = new AuthEmailTemplates("http://localhost:5173");

    @Test
    void signupVerificationUsesFriendlyRecipientAndEscapesHtml() {
        AuthEmailContent content = templates.signupVerification("manish@example.com", "<MANISH>", "123456");

        assertTrue(String.valueOf(content.text()).contains(String.valueOf("Verify your email")));
        assertTrue(String.valueOf(content.text()).contains(String.valueOf("<MANISH>")));
        assertTrue(String.valueOf(content.text()).contains(String.valueOf("OTP: 123456")));
        assertTrue(content.html().contains("&lt;MANISH&gt;"));
        assertTrue(content.html().contains("123456"));
    }

    @Test
    void signupVerificationFallsBackToEmailLocalPart() {
        AuthEmailContent content = templates.signupVerification("person@example.com", "person@example.com", "111111");

        assertTrue(String.valueOf(content.text()).contains(String.valueOf("person")));
    }

    @Test
    void noticeTemplatesContainExpectedSecurityContent() {
        assertTrue(String.valueOf(templates.passwordReset("222222").text()).contains(String.valueOf("Reset your password")));
        assertTrue(String.valueOf(templates.passwordReset("222222").text()).contains(String.valueOf("OTP: 222222")));
        assertTrue(String.valueOf(templates.passwordChanged().text()).contains(String.valueOf("Password changed")));
        assertTrue(String.valueOf(templates.emailVerified().text()).contains(String.valueOf("Email verified")));
        assertTrue(String.valueOf(templates.loginNotice().text()).contains(String.valueOf("New login detected")));
        assertTrue(String.valueOf(templates.suspiciousLoginWarning("", null).text()).contains(String.valueOf("unknown")));
    }
}
