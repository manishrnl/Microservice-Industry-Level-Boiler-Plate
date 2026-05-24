package com.company.platform.gateway.controller;

import com.company.platform.gateway.log.GatewayLogEntry;
import com.company.platform.gateway.log.GatewayLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilityLogController {
    private final GatewayLogService logs;
    private final ReactiveJwtDecoder jwtDecoder;

    public ObservabilityLogController(GatewayLogService logs, ReactiveJwtDecoder jwtDecoder) {
        this.logs = logs;
        this.jwtDecoder = jwtDecoder;
    }

    @GetMapping("/logs")
    public Mono<Map<String, Object>> logs(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                          @RequestParam(value = "q", required = false) String query,
                                          @RequestParam(value = "level", required = false) String level,
                                          @RequestParam(value = "category", required = false) String category,
                                          @RequestParam(value = "service", required = false) String service,
                                          @RequestParam(value = "status", required = false) Integer status,
                                          @RequestParam(value = "limit", defaultValue = "200") int limit) {
        return requireAdmin(authorization)
                .map(jwt -> {
                    List<GatewayLogEntry> items = logs.search(query, level, category, service, status, limit);
                    return Map.of(
                            "items", items,
                            "count", items.size(),
                            "limit", Math.max(1, Math.min(limit, 500))
                    );
                });
    }

    private Mono<Jwt> requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token"));
        }
        return jwtDecoder.decode(authorization.substring(7))
                .flatMap(jwt -> {
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    boolean allowed = roles != null && (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN"));
                    return allowed
                            ? Mono.just(jwt)
                            : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required"));
                })
                .onErrorMap(ex -> ex instanceof ResponseStatusException ? ex : new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token"));
    }
}
