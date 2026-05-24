package com.company.platform.auth.email;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Service
public class BrevoMailDiagnosticsService {
    private static final URI BREVO_ACCOUNT_URI = URI.create("https://api.brevo.com/v3/account");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final String provider;
    private final String from;
    private final String fromName;
    private final String brevoApiKey;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean smtpAuth;
    private final boolean sslEnable;
    private final boolean starttlsEnable;
    private final boolean starttlsRequired;

    public BrevoMailDiagnosticsService(
            ObjectMapper objectMapper,
            @Value("${app.mail.provider:smtp}") String provider,
            @Value("${app.mail.from:no-reply@platform.local}") String from,
            @Value("${app.mail.from-name:Platform}") String fromName,
            @Value("${app.mail.brevo.api-key:}") String brevoApiKey,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:0}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password,
            @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean smtpAuth,
            @Value("${spring.mail.properties.mail.smtp.ssl.enable:false}") boolean sslEnable,
            @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}") boolean starttlsEnable,
            @Value("${spring.mail.properties.mail.smtp.starttls.required:false}") boolean starttlsRequired) {
        this.objectMapper = objectMapper;
        this.provider = provider;
        this.from = from;
        this.fromName = fromName;
        this.brevoApiKey = brevoApiKey;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.smtpAuth = smtpAuth;
        this.sslEnable = sslEnable;
        this.starttlsEnable = starttlsEnable;
        this.starttlsRequired = starttlsRequired;
    }

    public Map<String, Object> diagnostics() {
        Map<String, Object> response = baseResponse();
        response.put("brevoAccount", checkBrevoAccount());
        response.put("smtpConnection", checkSmtp(false, null));
        return response;
    }

    public Map<String, Object> sendTest(String to) {
        Map<String, Object> response = baseResponse();
        response.put("brevoAccount", checkBrevoAccount());
        response.put("smtpDelivery", checkSmtp(true, to));
        return response;
    }

    private Map<String, Object> baseResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("checkedAt", Instant.now().toString());
        response.put("configuration", configuration());
        response.put("recommendations", recommendations());
        return response;
    }

    private Map<String, Object> configuration() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("provider", provider);
        config.put("fromEmail", maskEmail(from));
        config.put("fromNameConfigured", StringUtils.hasText(fromName));
        config.put("host", host);
        config.put("port", port);
        config.put("smtpAuth", smtpAuth);
        config.put("sslEnable", sslEnable);
        config.put("starttlsEnable", starttlsEnable);
        config.put("starttlsRequired", starttlsRequired);
        config.put("usernameConfigured", StringUtils.hasText(username));
        config.put("usernamePreview", maskEmail(username));
        config.put("passwordConfigured", StringUtils.hasText(password));
        config.put("smtpPasswordKind", keyKind(password));
        config.put("brevoApiKeyConfigured", StringUtils.hasText(brevoApiKey));
        config.put("brevoApiKeyKind", keyKind(brevoApiKey));
        config.put("portSecurityPairValid", portSecurityPairValid());
        return config;
    }

    private List<String> recommendations() {
        List<String> recommendations = new ArrayList<>();
        String apiKeyKind = keyKind(brevoApiKey);
        if ("brevo".equalsIgnoreCase(provider) && !"rest-api-key".equals(apiKeyKind)) {
            recommendations.add("MAIL_PROVIDER=brevo uses the Brevo REST API first. Set MAIL_PROVIDER=smtp for Brevo SMTP, or use a REST API key that starts with xkeysib-.");
        }
        if ("smtp-key".equals(apiKeyKind)) {
            recommendations.add("BREVO_API_KEY contains an SMTP key. REST API calls need a Brevo API key that starts with xkeysib-.");
        }
        if (port == 587 && sslEnable) {
            recommendations.add("Port 587 must use STARTTLS, not implicit SSL. Set MAIL_SMTP_SSL_ENABLE=false and keep MAIL_SMTP_STARTTLS_ENABLE=true.");
        }
        if (port == 465 && !sslEnable) {
            recommendations.add("Port 465 must use implicit SSL. Set MAIL_SMTP_SSL_ENABLE=true and MAIL_SMTP_STARTTLS_ENABLE=false.");
        }
        if (port == 465 && starttlsEnable) {
            recommendations.add("STARTTLS is not needed on port 465 because the connection starts inside TLS.");
        }
        if (!StringUtils.hasText(username)) {
            recommendations.add("BREVO_USERNAME is empty.");
        }
        if (!StringUtils.hasText(password)) {
            recommendations.add("BREVO_PASSWORD is empty.");
        }
        if (!StringUtils.hasText(from)) {
            recommendations.add("BREVO_FROM_EMAIL or MAIL_FROM is empty.");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Configuration shape looks valid. If delivery still fails, check sender verification, SMTP key status, and Brevo transactional logs.");
        }
        return recommendations;
    }

    private Map<String, Object> checkBrevoAccount() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("checked", false);
        if (!StringUtils.hasText(brevoApiKey)) {
            result.put("skippedReason", "BREVO_API_KEY is empty.");
            return result;
        }
        if (!"rest-api-key".equals(keyKind(brevoApiKey))) {
            result.put("skippedReason", "BREVO_API_KEY does not look like a REST API key. Brevo REST keys usually start with xkeysib-.");
            return result;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(BREVO_ACCOUNT_URI)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            result.put("checked", true);
            result.put("statusCode", response.statusCode());
            result.put("rateLimit", rateLimitHeaders(response));
            result.put("success", response.statusCode() >= 200 && response.statusCode() < 300);
            Object body = parseJson(response.body());
            if (Boolean.TRUE.equals(result.get("success"))) {
                result.put("plan", extract(body, "plan"));
                result.put("relay", extract(body, "relay"));
            } else {
                result.put("errorBody", redact(body));
                result.put("diagnosis", diagnosisForBrevoStatus(response.statusCode(), body));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            result.put("errorType", ex.getClass().getSimpleName());
            result.put("message", "Brevo account check was interrupted.");
        } catch (Exception ex) {
            result.put("errorType", ex.getClass().getSimpleName());
            result.put("message", safeMessage(ex));
        }
        return result;
    }

    private Map<String, Object> checkSmtp(boolean sendMessage, String to) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("checked", false);
        result.put("messageSent", false);
        if (!StringUtils.hasText(host)) {
            result.put("skippedReason", "MAIL_HOST is empty.");
            return result;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            result.put("skippedReason", "BREVO_USERNAME or BREVO_PASSWORD is empty.");
            return result;
        }
        if (sendMessage && !StringUtils.hasText(to)) {
            result.put("skippedReason", "A recipient email is required for send-test.");
            return result;
        }

        Transport transport = null;
        long start = System.nanoTime();
        try {
            Session session = Session.getInstance(smtpProperties());
            session.setDebug(false);
            transport = session.getTransport("smtp");
            transport.connect(host, port, username, password);
            result.put("checked", true);
            result.put("authenticated", true);
            if (sendMessage) {
                MimeMessage message = testMessage(session, to);
                transport.sendMessage(message, message.getAllRecipients());
                result.put("messageSent", true);
                result.put("recipient", maskEmail(to));
            }
            result.put("success", true);
        } catch (AuthenticationFailedException ex) {
            result.put("checked", true);
            result.put("errorType", ex.getClass().getSimpleName());
            result.put("message", "SMTP authentication failed. Check BREVO_USERNAME and BREVO_PASSWORD, and confirm the SMTP key is enabled in Brevo.");
        } catch (MessagingException ex) {
            result.put("checked", true);
            result.put("errorType", ex.getClass().getSimpleName());
            result.put("message", safeMessage(ex));
            result.put("diagnosis", smtpDiagnosis(ex));
        } catch (Exception ex) {
            result.put("checked", true);
            result.put("errorType", ex.getClass().getSimpleName());
            result.put("message", safeMessage(ex));
        } finally {
            closeTransport(transport);
            result.put("durationMs", Duration.ofNanos(System.nanoTime() - start).toMillis());
        }
        return result;
    }

    private Properties smtpProperties() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.auth", String.valueOf(smtpAuth));
        properties.put("mail.smtp.ssl.enable", String.valueOf(sslEnable));
        properties.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        properties.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return properties;
    }

    private MimeMessage testMessage(Session session, String to) throws Exception {
        MimeMessage message = new MimeMessage(session);
        InternetAddress sender = new InternetAddress(from);
        if (StringUtils.hasText(fromName)) {
            sender.setPersonal(fromName, StandardCharsets.UTF_8.name());
        }
        message.setFrom(sender);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject("Brevo SMTP diagnostic test", StandardCharsets.UTF_8.name());
        message.setText("Brevo SMTP diagnostic test sent at " + Instant.now(), StandardCharsets.UTF_8.name());
        message.setSentDate(Date.from(Instant.now()));
        return message;
    }

    private Map<String, Object> rateLimitHeaders(HttpResponse<?> response) {
        Map<String, Object> headers = new LinkedHashMap<>();
        response.headers().firstValue("x-sib-ratelimit-limit").ifPresent(value -> headers.put("limit", value));
        response.headers().firstValue("x-sib-ratelimit-remaining").ifPresent(value -> headers.put("remaining", value));
        response.headers().firstValue("x-sib-ratelimit-reset").ifPresent(value -> headers.put("reset", value));
        return headers;
    }

    private Object parseJson(String body) {
        if (!StringUtils.hasText(body)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return body.length() > 500 ? body.substring(0, 500) : body;
        }
    }

    private Object extract(Object body, String key) {
        if (body instanceof Map<?, ?> map && map.containsKey(key)) {
            return redact(map.get(key));
        }
        return Map.of();
    }

    private Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> redacted = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                String keyText = String.valueOf(key);
                if (sensitiveKey(keyText)) {
                    redacted.put(keyText, "<redacted>");
                } else {
                    redacted.put(keyText, redact(mapValue));
                }
            });
            return redacted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::redact).toList();
        }
        if (value instanceof String text && text.length() > 500) {
            return text.substring(0, 500);
        }
        return value;
    }

    private boolean sensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("key")
                || normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("token");
    }

    private String diagnosisForBrevoStatus(int statusCode, Object body) {
        if (statusCode == 401) {
            return "Brevo rejected the REST API key. Use a valid API key that starts with xkeysib-, or use MAIL_PROVIDER=smtp to avoid REST API sending.";
        }
        if (statusCode == 402) {
            return "Brevo says the account needs activation or additional credits.";
        }
        if (body instanceof Map<?, ?> map && String.valueOf(map.get("message")).toLowerCase(Locale.ROOT).contains("sender")) {
            return "Brevo rejected the sender. Verify BREVO_FROM_EMAIL in Brevo.";
        }
        return "Brevo REST API returned a non-success status.";
    }

    private String smtpDiagnosis(Exception ex) {
        String message = safeMessage(ex).toLowerCase(Locale.ROOT);
        if (message.contains("unsupported or unrecognized ssl message") && port == 587 && sslEnable) {
            return "The app is using implicit SSL on port 587. Use STARTTLS on 587 by setting MAIL_SMTP_SSL_ENABLE=false.";
        }
        if (message.contains("could not convert socket to tls")) {
            return "STARTTLS negotiation failed. Check MAIL_SMTP_STARTTLS_ENABLE and the selected SMTP port.";
        }
        if (message.contains("connection timed out") || message.contains("could not connect")) {
            return "The container could not connect to the SMTP host and port. Check network access and port/security settings.";
        }
        return "SMTP connection or send failed. Check host, port, encryption, sender, and Brevo SMTP key status.";
    }

    private boolean portSecurityPairValid() {
        if (port == 587 || port == 2525) {
            return !sslEnable && starttlsEnable;
        }
        if (port == 465) {
            return sslEnable && !starttlsEnable;
        }
        return false;
    }

    private String keyKind(String value) {
        if (!StringUtils.hasText(value)) {
            return "empty";
        }
        if (value.startsWith("xkeysib-")) {
            return "rest-api-key";
        }
        if (value.startsWith("xsmtpsib-")) {
            return "smtp-key";
        }
        return "unknown";
    }

    private String maskEmail(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int atIndex = value.indexOf('@');
        if (atIndex > 0) {
            String local = value.substring(0, atIndex);
            String domain = value.substring(atIndex + 1);
            String prefix = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
            return prefix + "***@" + domain;
        }
        if (value.length() <= 8) {
            return "<configured>";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }

    private String safeMessage(Throwable ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private void closeTransport(Transport transport) {
        if (transport == null) {
            return;
        }
        try {
            if (transport.isConnected()) {
                transport.close();
            }
        } catch (MessagingException ignored) {
            // Closing a failed diagnostic connection should not hide the real result.
        }
    }
}
