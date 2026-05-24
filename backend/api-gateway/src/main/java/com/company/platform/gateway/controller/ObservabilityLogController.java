package com.company.platform.gateway.controller;

import com.company.platform.gateway.log.GatewayLogEntry;
import com.company.platform.gateway.log.GatewayLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/observability")
public class ObservabilityLogController {
    private final GatewayLogService logs;
    private final ReactiveJwtDecoder jwtDecoder;
    private final WebClient lokiClient;

    public ObservabilityLogController(GatewayLogService logs,
                                      ReactiveJwtDecoder jwtDecoder,
                                      WebClient.Builder webClientBuilder,
                                      @Value("${observability.loki-url:${LOKI_URL:http://loki:3100}}") String lokiUrl) {
        this.logs = logs;
        this.jwtDecoder = jwtDecoder;
        this.lokiClient = webClientBuilder.baseUrl(lokiUrl).build();
    }

    @GetMapping("/logs")
    public Mono<Map<String, Object>> logs(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
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

    @GetMapping("/loki/labels")
    public Mono<Map> lokiLabels(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return requireAdmin(authorization)
                .then(lokiClient.get()
                        .uri("/loki/api/v1/labels")
                        .retrieve()
                        .bodyToMono(Map.class));
    }

    @GetMapping("/loki/services")
    public Mono<Map> lokiServices(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return requireAdmin(authorization)
                .then(lokiClient.get()
                        .uri("/loki/api/v1/label/service/values")
                        .retrieve()
                        .bodyToMono(Map.class));
    }

    @GetMapping("/loki/query-range")
    public Mono<Map> lokiQueryRange(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                    @RequestParam(value = "query", required = false) String query,
                                    @RequestParam(value = "limit", defaultValue = "200") int limit,
                                    @RequestParam(value = "direction", defaultValue = "BACKWARD") String direction,
                                    @RequestParam(value = "start", required = false) String start,
                                    @RequestParam(value = "end", required = false) String end) {
        String logql = query == null || query.isBlank() ? "{compose_project=\"microservice-industry\"}" : query;
        int cappedLimit = Math.max(1, Math.min(limit, 1000));
        String safeDirection = "FORWARD".equalsIgnoreCase(direction) ? "FORWARD" : "BACKWARD";
        return requireAdmin(authorization)
                .then(lokiClient.get()
                        .uri(builder -> builder
                                .path("/loki/api/v1/query_range")
                                .queryParam("query", logql)
                                .queryParam("limit", cappedLimit)
                                .queryParam("direction", safeDirection)
                                .queryParamIfPresent("start", optionalText(start))
                                .queryParamIfPresent("end", optionalText(end))
                                .build())
                        .retrieve()
                        .bodyToMono(Map.class));
    }

    private Optional<String> optionalText(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
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
