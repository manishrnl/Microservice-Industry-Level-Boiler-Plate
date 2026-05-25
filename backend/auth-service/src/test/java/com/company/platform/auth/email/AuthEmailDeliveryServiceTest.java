package com.company.platform.auth.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthEmailDeliveryServiceTest {

    @Test
    void sendNowSendsHtmlMimeMessageThroughSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        given(sender.createMimeMessage()).willReturn(message);
        AuthEmailDeliveryService service = smtpService(sender);

        ReflectionTestUtils.invokeMethod(service, "sendNow", "user@example.com", "Subject", new AuthEmailContent("Text", "<b>Html</b>"));

        verify(sender).send(message);
        service.shutdown();
    }

    @Test
    void sendNowFallsBackToPlainTextWhenMimeSendFails() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        given(sender.createMimeMessage()).willReturn(message);
        doThrow(new MailSendException("smtp down")).when(sender).send(message);
        AuthEmailDeliveryService service = smtpService(sender);

        ReflectionTestUtils.invokeMethod(service, "sendNow", "user@example.com", "Subject", new AuthEmailContent("Text", "<b>Html</b>"));

        verify(sender).send(any(SimpleMailMessage.class));
        service.shutdown();
    }

    @Test
    void preferBrevoStopsAfterAcceptedRestApiResponse() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        given(response.statusCode()).willReturn(202);
        given(response.body()).willReturn("{}");
        given(httpClient.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).willReturn(response);
        AuthEmailDeliveryService service = new AuthEmailDeliveryService(
                sender,
                new ObjectMapper(),
                "no-reply@example.com",
                "Platform",
                "xkeysib-test",
                "brevo"
        );
        ReflectionTestUtils.setField(service, "httpClient", httpClient);

        ReflectionTestUtils.invokeMethod(service, "sendNow", "user@example.com", "Subject", new AuthEmailContent("Text", "<b>Html</b>"));

        verify(sender, never()).createMimeMessage();
        verify(httpClient).send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        service.shutdown();
    }

    private AuthEmailDeliveryService smtpService(JavaMailSender sender) {
        return new AuthEmailDeliveryService(
                sender,
                new ObjectMapper(),
                "no-reply@example.com",
                "Platform",
                "",
                "smtp"
        );
    }
}
