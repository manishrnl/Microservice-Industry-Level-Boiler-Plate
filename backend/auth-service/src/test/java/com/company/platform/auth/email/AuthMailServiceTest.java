package com.company.platform.auth.email;

import com.company.platform.commons.exception.ApiExceptions;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthMailServiceTest {
    private final AuthEmailDeliveryService delivery = mock(AuthEmailDeliveryService.class);
    private final AuthMailService service = new AuthMailService(delivery, new AuthEmailTemplates("http://localhost:5173"));

    @Test
    void signupOtpCanBeVerifiedOnceAndRateLimitedForResend() {
        String otp = service.sendSignupVerification("Manish@Example.com", "MANISH");

        assertEquals(otp.length(), 6);
        assertTrue(service.sendSignupVerificationIfNeeded("manish@example.com", "MANISH").isEmpty());
        expectThrows(
                ApiExceptions.RateLimitExceededException.class,
                () -> service.resendSignupVerification("manish@example.com", "MANISH")
        );
        assertFalse(service.verifySignupOtp("manish@example.com", "bad"));
        assertTrue(service.verifySignupOtp("manish@example.com", otp));
        assertFalse(service.verifySignupOtp("manish@example.com", otp));
        verify(delivery).enqueue(eq("Manish@Example.com"), eq("Verify your platform account"), any(AuthEmailContent.class));
    }

    @Test
    void passwordResetOtpIsConsumedAndNotificationEmailsAreQueued() {
        String otp = service.sendPasswordReset("user@example.com");

        assertTrue(service.consumePasswordResetOtp("user@example.com", otp));
        assertFalse(service.consumePasswordResetOtp("user@example.com", otp));
        service.sendPasswordChanged("user@example.com");
        service.sendEmailVerified("user@example.com");
        service.sendLoginNotice("user@example.com");
        service.sendSuspiciousLoginWarning("user@example.com", "127.0.0.1", "Chrome");

        verify(delivery).enqueue(eq("user@example.com"), eq("Reset your platform password"), any(AuthEmailContent.class));
        verify(delivery).enqueue(eq("user@example.com"), eq("Your platform password was changed"), any(AuthEmailContent.class));
        verify(delivery).enqueue(eq("user@example.com"), eq("Your platform email is verified"), any(AuthEmailContent.class));
        verify(delivery).enqueue(eq("user@example.com"), eq("New login to your platform account"), any(AuthEmailContent.class));
        verify(delivery).enqueue(eq("user@example.com"), eq("Security warning for your platform account"), any(AuthEmailContent.class));
    }
}
