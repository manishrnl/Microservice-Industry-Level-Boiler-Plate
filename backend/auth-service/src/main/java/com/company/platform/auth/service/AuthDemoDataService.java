package com.company.platform.auth.service;

import com.company.platform.auth.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthDemoDataService {
    private static final Logger log = LoggerFactory.getLogger(AuthDemoDataService.class);

    private final RestClient restClient;
    private final Map<String, String> seedEndpoints;

    public AuthDemoDataService(RestClient.Builder restClientBuilder,
                               @Value("${app.user-service-url:http://user-service:8082}") String userServiceUrl,
                               @Value("${app.notification-service-url:http://notification-service:8083}") String notificationServiceUrl,
                               @Value("${app.payment-service-url:http://payment-service:8084}") String paymentServiceUrl,
                               @Value("${app.file-service-url:http://file-service:8085}") String fileServiceUrl,
                               @Value("${app.ai-service-url:http://ai-service:8086}") String aiServiceUrl,
                               @Value("${app.audit-service-url:http://audit-service:8087}") String auditServiceUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
        this.seedEndpoints = new LinkedHashMap<>();
        seedEndpoints.put("user-service", endpoint(userServiceUrl, "/api/v1/users/internal/demo-data"));
        seedEndpoints.put("notification-service", endpoint(notificationServiceUrl, "/api/v1/notifications/internal/demo-data"));
        seedEndpoints.put("payment-service", endpoint(paymentServiceUrl, "/api/v1/payments/internal/demo-data"));
        seedEndpoints.put("file-service", endpoint(fileServiceUrl, "/api/v1/files/internal/demo-data"));
        seedEndpoints.put("ai-service", endpoint(aiServiceUrl, "/api/v1/ai/internal/demo-data"));
        seedEndpoints.put("audit-service", endpoint(auditServiceUrl, "/api/v1/audit/internal/demo-data"));
    }

    @Async
    public void provision(User user) {
        provision(user, null, null);
    }

    @Async
    public void provision(User user, String name, String avatarUrl) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getId());
        body.put("email", user.getEmail());
        body.put("name", firstText(name, displayName(user)));
        body.put("username", user.getUsername());
        body.put("avatarUrl", blankToNull(avatarUrl));
        seedEndpoints.forEach((service, url) -> postSeedRequest(service, url, body));
    }

    private void postSeedRequest(String service, String url, Map<String, Object> body) {
        try {
            restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not seed demo data via {} at {}", service, url, ex);
        }
    }

    private String endpoint(String baseUrl, String path) {
        String base = baseUrl == null || baseUrl.isBlank() ? "" : baseUrl.trim();
        return StringUtils.trimTrailingCharacter(base, '/') + path;
    }

    private String displayName(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank() && !user.getUsername().equalsIgnoreCase(user.getEmail())) {
            return user.getUsername();
        }
        String email = user.getEmail();
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    private String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
