package com.company.platform.gateway.filter;

import com.company.platform.gateway.log.GatewayLogService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/.well-known/jwks.json",
            "/api/v1/auth/db-ping",
            "/api/v1/auth/db-stats",
            "/api/v1/auth/oauth2",
            "/api/v1/payments/webhook",
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );
    private final ReactiveJwtDecoder jwtDecoder;
    private final GatewayLogService gatewayLogs;

    public JwtAuthenticationFilter(ReactiveJwtDecoder jwtDecoder, GatewayLogService gatewayLogs) {
        this.jwtDecoder = jwtDecoder;
        this.gatewayLogs = gatewayLogs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "Missing bearer token");
        }
        return jwtDecoder.decode(authorization.substring(7))
                .flatMap(jwt -> {
                    String roles = String.join(",", jwt.getClaimAsStringList("roles") == null ? List.of() : jwt.getClaimAsStringList("roles"));
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-Id", jwt.getSubject())
                            .header("X-User-Email", jwt.getClaimAsString("email"))
                            .header("X-User-Name", jwt.getClaimAsString("name"))
                            .header("X-User-Roles", roles)
                            .header("X-Session-Id", jwt.getClaimAsString("sessionId"))
                            .build();
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .onErrorResume(ex -> unauthorized(exchange, "Invalid or expired token"));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String detail) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        gatewayLogs.recordSecurity(exchange.getRequest().getPath().value(), detail, HttpStatus.UNAUTHORIZED.value());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        String body = "{\"type\":\"https://httpstatuses.com/401\",\"title\":\"Unauthorized\",\"status\":401,\"detail\":\"" + detail + "\",\"instance\":\"" + exchange.getRequest().getPath().value() + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
