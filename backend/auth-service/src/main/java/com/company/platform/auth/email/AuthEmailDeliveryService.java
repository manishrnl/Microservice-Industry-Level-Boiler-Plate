package com.company.platform.auth.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AuthEmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(AuthEmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final String from;
    private final String fromName;
    private final String brevoApiKey;
    private final boolean preferBrevo;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "auth-mail-sender");
        thread.setDaemon(true);
        return thread;
    });

    public AuthEmailDeliveryService(JavaMailSender mailSender,
                                    ObjectMapper objectMapper,
                                    @Value("${app.mail.from:no-reply@platform.local}") String from,
                                    @Value("${app.mail.from-name:Platform}") String fromName,
                                    @Value("${app.mail.brevo.api-key:}") String brevoApiKey,
                                    @Value("${app.mail.provider:auto}") String mailProvider) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.from = from;
        this.fromName = fromName;
        this.brevoApiKey = brevoApiKey;
        this.preferBrevo = "brevo".equalsIgnoreCase(mailProvider);
    }

    public void enqueue(String to, String subject, AuthEmailContent content) {
        CompletableFuture.runAsync(() -> sendNow(to, subject, content), mailExecutor)
                .exceptionally(ex -> {
                    log.warn("Queued auth email could not be delivered to {}", to, ex);
                    return null;
                });
    }

    private void sendNow(String to, String subject, AuthEmailContent content) {
        if (preferBrevo && sendWithBrevo(to, subject, content)) {
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content.text(), content.html());
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("SMTP auth email could not be sent to {}", to, ex);
            if (!preferBrevo && sendWithBrevo(to, subject, content)) {
                return;
            }
            sendPlainTextFallback(to, subject, content.text());
            log.warn("No auth email provider delivered message to {}", to);
        } catch (MessagingException ex) {
            log.warn("HTML auth email could not be created for {}", to, ex);
            sendPlainTextFallback(to, subject, content.text());
        }
    }

    private void sendPlainTextFallback(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Plain text auth email fallback could not be sent to {}", to, ex);
        }
    }

    private boolean sendWithBrevo(String to, String subject, AuthEmailContent content) {
        if (!StringUtils.hasText(brevoApiKey)) {
            return false;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", from),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "textContent", content.text(),
                    "htmlContent", content.html()
            );
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10))
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("api-key", brevoApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            }
            log.warn("Brevo auth email failed for {} with status {}", to, response.statusCode());
        } catch (IOException ex) {
            log.warn("Brevo auth email payload could not be created for {}", to, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Brevo auth email was interrupted for {}", to, ex);
        }
        return false;
    }

    @PreDestroy
    void shutdown() {
        mailExecutor.shutdown();
    }
}
