package com.company.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    private static final List<String> DEFAULT_EXCLUDED_PATHS = List.of(
            "/actuator",
            "/api/v1/auth/.well-known/jwks.json",
            "/api/v1/auth/db-ping",
            "/api/v1/auth/db-stats"
    );

    private final ReactiveStringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final boolean includeProbePaths;
    private final boolean failOpen;
    private final long anonymousLimit;
    private final long authenticatedLimit;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate,
                           @Value("${gateway.rate-limit.enabled:true}") boolean enabled,
                           @Value("${gateway.rate-limit.include-probe-paths:false}") boolean includeProbePaths,
                           @Value("${gateway.rate-limit.fail-open:true}") boolean failOpen,
                           @Value("${gateway.rate-limit.anonymous-requests-per-minute:100}") long anonymousLimit,
                           @Value("${gateway.rate-limit.authenticated-requests-per-minute:1000}") long authenticatedLimit) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.includeProbePaths = includeProbePaths;
        this.failOpen = failOpen;
        this.anonymousLimit = anonymousLimit;
        this.authenticatedLimit = authenticatedLimit;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!enabled || (!includeProbePaths && DEFAULT_EXCLUDED_PATHS.stream().anyMatch(path::startsWith))) {
            return chain.filter(exchange);
        }

        String remoteAddress = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String user = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .orElse("ip:" + remoteAddress);
        String key = "ratelimit:" + user + ":" + (Instant.now().getEpochSecond() / 60);
        long limit = user.startsWith("ip:") ? anonymousLimit : authenticatedLimit;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> count != null && count == 1
                        ? redisTemplate.expire(key, Duration.ofMinutes(1)).thenReturn(count)
                        : Mono.just(count == null ? 0L : count))
                .flatMap(count -> {
                    exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
                    exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - count)));
                    exchange.getResponse().getHeaders().set("X-RateLimit-Reset", "60");
                    if (count > limit) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap("{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8));
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> failOpen ? chain.filter(exchange) : Mono.error(ex));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
