package com.company.platform.auth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthEmailTemplates {
    private final String frontendBaseUrl;

    public AuthEmailTemplates(@Value("${app.frontend-base-url:https://manishrnl-microservice-template.netlify.app}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public AuthEmailContent signupVerification(String email, String name, String otp) {
        return otp(
                "Verify your email",
                "Welcome to the platform, " + recipientName(name, email) + ".",
                "Use this one-time code to activate your account.",
                otp,
                "Open " + frontendBaseUrl + " and enter this code. It expires in 10 minutes."
        );
    }

    public AuthEmailContent passwordReset(String otp) {
        return otp(
                "Reset your password",
                "We received a password reset request for your account.",
                "Use this one-time code to continue.",
                otp,
                "If you did not request this, you can safely ignore this email."
        );
    }

    public AuthEmailContent passwordChanged() {
        return notice(
                "Password changed",
                "Your password was changed successfully.",
                "If this was not you, reset your password immediately and review your active sessions."
        );
    }

    public AuthEmailContent emailVerified() {
        return notice(
                "Email verified",
                "Your email address has been verified successfully.",
                "You can now sign in and use your platform account."
        );
    }

    public AuthEmailContent loginNotice() {
        return notice(
                "New login detected",
                "Your account was used to sign in.",
                "If this was you, no action is needed. If this was not you, reset your password now."
        );
    }

    private AuthEmailContent otp(String title, String intro, String body, String otp, String footer) {
        String text = String.join("\n",
                title,
                "",
                intro,
                body,
                "",
                "OTP: " + otp,
                "",
                footer);
        return new AuthEmailContent(text, template(title, intro, body, otp, footer));
    }

    private AuthEmailContent notice(String title, String intro, String footer) {
        String text = String.join("\n",
                title,
                "",
                intro,
                "",
                footer);
        return new AuthEmailContent(text, template(title, intro, "", null, footer));
    }

    private String template(String title, String intro, String body, String otp, String footer) {
        String otpBlock = StringUtils.hasText(otp)
                ? """
                <div style="margin:28px 0;padding:20px;border-radius:16px;background:#0f172a;text-align:center;">
                  <div style="font-size:12px;font-weight:700;letter-spacing:0.18em;text-transform:uppercase;color:#67e8f9;">One-time code</div>
                  <div style="margin-top:10px;font-size:34px;line-height:1;font-weight:800;letter-spacing:0.24em;color:#ffffff;">%s</div>
                </div>
                """.formatted(escapeHtml(otp))
                : "";
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#eef2f7;padding:32px 16px;font-family:Inter,Segoe UI,Arial,sans-serif;color:#0f172a;">
                  <div style="max-width:560px;margin:0 auto;border-radius:24px;overflow:hidden;background:#ffffff;box-shadow:0 24px 60px rgba(15,23,42,0.14);">
                    <div style="padding:28px 32px;background:linear-gradient(135deg,#0f172a,#134e4a);color:#ffffff;">
                      <div style="font-size:13px;font-weight:700;letter-spacing:0.16em;text-transform:uppercase;color:#99f6e4;">%s</div>
                      <h1 style="margin:14px 0 0;font-size:28px;line-height:1.2;font-weight:800;">%s</h1>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0;font-size:16px;line-height:1.7;color:#334155;">%s</p>
                      %s
                      <p style="margin:0;font-size:15px;line-height:1.7;color:#475569;">%s</p>
                      <div style="margin-top:28px;padding-top:20px;border-top:1px solid #e2e8f0;font-size:13px;line-height:1.6;color:#64748b;">%s</div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml("Platform"),
                escapeHtml(title),
                escapeHtml(intro),
                otpBlock,
                escapeHtml(body),
                escapeHtml(footer)
        );
    }

    private String recipientName(String name, String email) {
        if (StringUtils.hasText(name) && !name.equalsIgnoreCase(email)) {
            return name;
        }
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "there";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
