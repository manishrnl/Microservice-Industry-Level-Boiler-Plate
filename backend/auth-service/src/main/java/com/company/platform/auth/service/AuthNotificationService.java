package com.company.platform.auth.service;

import com.company.platform.auth.dto.ClientRequestMetadataDto;
import com.company.platform.auth.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class AuthNotificationService {
    private static final Logger log = LoggerFactory.getLogger(AuthNotificationService.class);

    private final RestClient restClient;
    private final String notificationServiceUrl;

    public AuthNotificationService(RestClient.Builder restClientBuilder,
                                   @Value("${app.notification-service-url:http://notification-service:8083}") String notificationServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void loginDetected(User user, String sessionId, ClientRequestMetadataDto metadata) {
        try {
            restClient.post()
                    .uri(notificationServiceUrl + "/api/v1/notifications/internal/login")
                    .body(Map.of(
                            "userId", user.getId(),
                            "email", user.getEmail(),
                            "name", user.getUsername() == null ? "" : user.getUsername(),
                            "sessionId", sessionId,
                            "ipAddress", metadata.getIpAddress() == null ? "" : metadata.getIpAddress(),
                            "userAgent", metadata.getUserAgent() == null ? "" : metadata.getUserAgent(),
                            "timeZone", metadata.getTimeZone() == null ? "" : metadata.getTimeZone(),
                            "localTime", metadata.getLocalTime() == null ? "" : metadata.getLocalTime(),
                            "loginAt", OffsetDateTime.now(ZoneOffset.UTC).toString()
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Could not persist login notification for userId={}", user.getId(), ex);
        }
    }
}
