package com.company.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String user = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-Id")).orElse("ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        String key = "ratelimit:" + user + ":" + LocalDateTime.now().withSecond(0).withNano(0);
        long limit = user.startsWith("ip:") ? 100 : 1000;
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> redisTemplate.expire(key, java.time.Duration.ofMinutes(1)).thenReturn(count))
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
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
