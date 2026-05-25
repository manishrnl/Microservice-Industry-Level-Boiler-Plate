package com.company.platform.auth.service;

import com.company.platform.auth.dto.ClientRequestMetadataDto;
import com.company.platform.auth.entity.User;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.testng.Assert.*;

class AuthOutboundServiceTest {
    private HttpServer server;
    private String baseUrl;
    private final List<String> paths = new CopyOnWriteArrayList<>();
    private final List<String> bodies = new CopyOnWriteArrayList<>();

    @BeforeMethod
    void startServer() throws IOException {
        paths.clear();
        bodies.clear();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterMethod
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loginNotificationPostsCompleteAuditPayloadAndDoesNotThrowOnTransportFailure() {
        AuthNotificationService service = new AuthNotificationService(RestClient.builder(), baseUrl);
        User user = user("u@example.com", null);

        service.loginDetected(user, "session-1", ClientRequestMetadataDto.builder()
                .ipAddress("127.0.0.1")
                .userAgent("Chrome")
                .timeZone("Asia/Kolkata")
                .localTime("2026-05-25 10:00")
                .build());
        new AuthNotificationService(RestClient.builder(), "http://127.0.0.1:1")
                .loginDetected(user, "session-2", ClientRequestMetadataDto.builder().build());

        assertEquals(paths.getFirst(), "/api/v1/notifications/internal/login");
        assertTrue(bodies.getFirst().contains("\"email\":\"u@example.com\""));
        assertTrue(bodies.getFirst().contains("\"name\":\"\""));
        assertTrue(bodies.getFirst().contains("\"sessionId\":\"session-1\""));
        assertTrue(bodies.getFirst().contains("\"timeZone\":\"Asia/Kolkata\""));
    }

    @Test
    void demoDataProvisionPostsAllSeedRequestsWithNormalizedUserPayload() {
        AuthDemoDataService service = new AuthDemoDataService(
                RestClient.builder(),
                baseUrl + "/",
                baseUrl,
                baseUrl,
                baseUrl,
                baseUrl,
                baseUrl
        );
        User user = user("demo@example.com", "demo");

        service.provision(user, " Demo User ", " /avatar.png ");

        assertEquals(paths.size(), 6);
        assertTrue(paths.contains("/api/v1/users/internal/demo-data"));
        assertTrue(paths.contains("/api/v1/notifications/internal/demo-data"));
        assertTrue(paths.contains("/api/v1/payments/internal/demo-data"));
        assertTrue(paths.contains("/api/v1/files/internal/demo-data"));
        assertTrue(paths.contains("/api/v1/ai/internal/demo-data"));
        assertTrue(paths.contains("/api/v1/audit/internal/demo-data"));
        String body = bodies.getFirst();
        assertTrue(body.contains("\"email\":\"demo@example.com\""));
        assertTrue(body.contains("\"name\":\"Demo User\""));
        assertTrue(body.contains("\"avatarUrl\":\"/avatar.png\""));
    }

    @Test
    void demoDataProvisionFallsBackToReadableNameAndNullAvatar() {
        AuthDemoDataService service = new AuthDemoDataService(RestClient.builder(), baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);
        User user = user("fallback@example.com", "fallback@example.com");

        service.provision(user, " ", " ");

        assertTrue(bodies.getFirst().contains("\"name\":\"fallback\""));
        assertTrue(bodies.getFirst().contains("\"avatarUrl\":null"));
    }

    private User user(String email, String username) {
        User user = User.builder()
                .email(email)
                .username(username)
                .provider("LOCAL")
                .accountStatus("ACTIVE")
                .build();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private void handle(HttpExchange exchange) throws IOException {
        paths.add(exchange.getRequestURI().getPath());
        bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
