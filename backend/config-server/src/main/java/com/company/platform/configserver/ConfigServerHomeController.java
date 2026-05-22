package com.company.platform.configserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
class ConfigServerHomeController {

    @GetMapping("/")
    Map<String, Object> home() {
        return Map.of(
                "service", "config-server",
                "status", "running",
                "message", "Use /{application}/{profile} to inspect resolved configuration.",
                "health", "/actuator/health",
                "metrics", "/actuator/prometheus",
                "examples", List.of(
                        "/application/default",
                        "/api-gateway/default",
                        "/auth-service/default",
                        "/user-service/default",
                        "/notification-service/default",
                        "/audit-service/default"
                )
        );
    }
}
