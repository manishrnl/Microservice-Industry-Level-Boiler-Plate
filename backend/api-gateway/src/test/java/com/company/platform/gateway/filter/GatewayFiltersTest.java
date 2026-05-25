package com.company.platform.gateway.filter;

import com.company.platform.gateway.log.GatewayLogService;
import org.testng.annotations.Test;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;

class GatewayFiltersTest {

    @Test
    void correlationFilterAddsRequestAndResponseHeaders() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me"));
        AtomicReference<String> requestTrace = new AtomicReference<>();
        GatewayFilterChain chain = chainedExchange -> {
            requestTrace.set(chainedExchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return chainedExchange.getResponse().setComplete();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(requestTrace.get());
        assertFalse(requestTrace.get().isBlank());
        assertEquals(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER), requestTrace.get());
        assertNull(MDC.get("traceId"));
        assertEquals(filter.getOrder(), Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void securityHeadersFilterAddsBrowserHardeningHeaders() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        filter.filter(exchange, next -> next.getResponse().setComplete()).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertEquals(headers.getFirst("X-Content-Type-Options"), "nosniff");
        assertEquals(headers.getFirst("X-Frame-Options"), "DENY");
        assertTrue(String.valueOf(headers.getFirst("Permissions-Policy")).contains(String.valueOf("geolocation=()")));
        assertEquals(filter.getOrder(), Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void jwtFilterSkipsOptionsAndPublicPaths() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(decoder, new GatewayLogService());
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            return Mono.empty();
        };

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/v1/private")), chain).block();
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/login")), chain).block();

        assertTrue(called.get());
        assertEquals(filter.getOrder(), Ordered.HIGHEST_PRECEDENCE + 1);
        verifyNoInteractions(decoder);
    }

    @Test
    void jwtFilterRejectsMissingTokenAndRecordsSecurityLog() {
        GatewayLogService logs = new GatewayLogService();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(ReactiveJwtDecoder.class), logs);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertEquals(exchange.getResponse().getStatusCode(), HttpStatus.UNAUTHORIZED);
        assertEquals(logs.search("Missing bearer token", "WARN", "SECURITY", "user-service", 401, 10).size(), 1);
    }

    @Test
    void jwtFilterAddsUserHeadersForValidToken() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", "user-1", "email", "admin@example.com", "name", "Admin", "roles", List.of("ADMIN"), "sessionId", "session-1"));
        given(decoder.decode("good")).willReturn(Mono.just(jwt));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(decoder, new GatewayLogService());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer good")
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();

        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertEquals(headers.getFirst("X-User-Id"), "user-1");
        assertEquals(headers.getFirst("X-User-Email"), "admin@example.com");
        assertEquals(headers.getFirst("X-User-Roles"), "ADMIN");
        assertEquals(headers.getFirst("X-Session-Id"), "session-1");
    }

    @Test
    void rateLimitSkipsDisabledAndProbePaths() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        RateLimitFilter disabled = new RateLimitFilter(redis, false, false, true, 1, 1);
        RateLimitFilter probesExcluded = new RateLimitFilter(redis, true, false, true, 1, 1);
        AtomicBoolean called = new AtomicBoolean(false);
        GatewayFilterChain chain = exchange -> {
            called.set(true);
            return Mono.empty();
        };

        disabled.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me")), chain).block();
        probesExcluded.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health")), chain).block();

        assertTrue(called.get());
        assertEquals(disabled.getOrder(), Ordered.HIGHEST_PRECEDENCE + 2);
        verifyNoInteractions(redis);
    }

    @Test
    void rateLimitRejectsRequestsOverLimitAndFailsOpenOnRedisErrors() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        given(redis.opsForValue()).willReturn(ops);
        given(ops.increment(anyString())).willReturn(Mono.just(2L), Mono.error(new IllegalStateException("redis down")));
        RateLimitFilter filter = new RateLimitFilter(redis, true, true, true, 1, 10);
        MockServerWebExchange limited = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me"));

        filter.filter(limited, ignored -> Mono.empty()).block();

        assertEquals(limited.getResponse().getStatusCode(), HttpStatus.TOO_MANY_REQUESTS);

        AtomicBoolean failOpenCalled = new AtomicBoolean(false);
        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users/me")), exchange -> {
            failOpenCalled.set(true);
            return Mono.empty();
        }).block();
        assertTrue(failOpenCalled.get());
    }

    @Test
    void requestLoggingRecordsCompletedRequestsWhenEnabled() {
        GatewayLogService logs = new GatewayLogService();
        RequestLoggingFilter filter = new RequestLoggingFilter(true, 1000, logs);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/files/my-files").header("X-User-Id", "user-1")
        );

        filter.filter(exchange, next -> {
            next.getResponse().setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }).block();

        assertEquals(logs.search("files", "INFO", "RUNTIME", "file-service", 200, 10).size(), 1);
        assertEquals(filter.getOrder(), Ordered.LOWEST_PRECEDENCE - 10);
    }

    @Test
    void requestLoggingSkipsRecordingWhenDisabledAndNoSlowThreshold() {
        GatewayLogService logs = new GatewayLogService();
        RequestLoggingFilter filter = new RequestLoggingFilter(false, 0, logs);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/files/my-files")), exchange -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
        assertTrue(logs.search("files", null, null, null, null, 10).isEmpty());
    }
}
