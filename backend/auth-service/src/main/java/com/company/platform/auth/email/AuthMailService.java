package com.company.platform.auth.email;

import com.company.platform.commons.exception.ApiExceptions;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthMailService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final AuthEmailDeliveryService delivery;
    private final AuthEmailTemplates templates;
    private final Map<String, OtpEntry> otpEntries = new ConcurrentHashMap<>();

    public AuthMailService(AuthEmailDeliveryService delivery, AuthEmailTemplates templates) {
        this.delivery = delivery;
        this.templates = templates;
    }

    public String sendSignupVerification(String email, String name) {
        return sendSignupVerification(email, name, true);
    }

    public String sendSignupVerificationIfNeeded(String email, String name) {
        return sendSignupVerification(email, name, false);
    }

    public String resendSignupVerification(String email, String name) {
        if (hasActiveOtp(email, Purpose.SIGNUP)) {
            throw new ApiExceptions.RateLimitExceededException("Please wait until the current 10-minute OTP expires before requesting a new one.");
        }
        return sendSignupVerification(email, name, true);
    }

    public String sendPasswordReset(String email) {
        String otp = otp();
        remember(email, Purpose.PASSWORD_RESET, otp);
        delivery.enqueue(email, "Reset your platform password", templates.passwordReset(otp));
        return otp;
    }

    public void sendPasswordChanged(String email) {
        delivery.enqueue(email, "Your platform password was changed", templates.passwordChanged());
    }

    public void sendEmailVerified(String email) {
        delivery.enqueue(email, "Your platform email is verified", templates.emailVerified());
    }

    public void sendLoginNotice(String email) {
        delivery.enqueue(email, "New login to your platform account", templates.loginNotice());
    }

    public boolean verifySignupOtp(String email, String otp) {
        return verify(email, Purpose.SIGNUP, otp, true);
    }

    public boolean consumePasswordResetOtp(String email, String otp) {
        return verify(email, Purpose.PASSWORD_RESET, otp, true);
    }

    private String sendSignupVerification(String email, String name, boolean forceNew) {
        if (!forceNew && hasActiveOtp(email, Purpose.SIGNUP)) {
            return "";
        }
        String otp = otp();
        remember(email, Purpose.SIGNUP, otp);
        delivery.enqueue(email, "Verify your platform account", templates.signupVerification(email, name, otp));
        return otp;
    }

    private void remember(String email, Purpose purpose, String otp) {
        otpEntries.put(key(email, purpose), new OtpEntry(otp, Instant.now().plus(OTP_TTL)));
    }

    private boolean hasActiveOtp(String email, Purpose purpose) {
        String key = key(email, purpose);
        OtpEntry entry = otpEntries.get(key);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            otpEntries.remove(key);
            return false;
        }
        return true;
    }

    private boolean verify(String email, Purpose purpose, String otp, boolean consume) {
        String key = key(email, purpose);
        OtpEntry entry = otpEntries.get(key);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            otpEntries.remove(key);
            return false;
        }
        if (!entry.otp().equals(otp)) {
            return false;
        }
        if (consume) {
            otpEntries.remove(key);
        }
        return true;
    }

    private String key(String email, Purpose purpose) {
        return purpose.name() + ":" + email.toLowerCase();
    }

    private String otp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private enum Purpose {
        SIGNUP,
        PASSWORD_RESET
    }

    private record OtpEntry(String otp, Instant expiresAt) {
    }
}
