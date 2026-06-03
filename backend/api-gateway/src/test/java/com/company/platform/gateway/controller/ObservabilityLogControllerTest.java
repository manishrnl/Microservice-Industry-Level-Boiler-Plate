package com.company.platform.gateway.controller;

import com.company.platform.gateway.log.GatewayLogService;
import org.testng.annotations.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class ObservabilityLogControllerTest {

    @Test
    void logsRequiresBearerToken() {
        ObservabilityLogController controller = new ObservabilityLogController(
                new GatewayLogService(),
                mock(ReactiveJwtDecoder.class),
                WebClient.builder(),
                "http://localhost:3100"
        );

        StepVerifier.create(controller.logs(null, null, null, null, null, null, 200))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertEquals(((ResponseStatusException) error).getStatusCode(), HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    void logsRejectsNonAdminRoles() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        given(decoder.decode("user-token")).willReturn(Mono.just(jwt(List.of("USER"))));
        ObservabilityLogController controller = new ObservabilityLogController(
                new GatewayLogService(),
                decoder,
                WebClient.builder(),
                "http://localhost:3100"
        );

        StepVerifier.create(controller.logs("Bearer user-token", null, null, null, null, null, 200))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertEquals(((ResponseStatusException) error).getStatusCode(), HttpStatus.FORBIDDEN);
                })
                .verify();
    }

    @Test
    void logsMapsDecoderFailuresToUnauthorized() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        given(decoder.decode("bad-token")).willReturn(Mono.error(new IllegalArgumentException("bad jwt")));
        ObservabilityLogController controller = new ObservabilityLogController(
                new GatewayLogService(),
                decoder,
                WebClient.builder(),
                "http://localhost:3100"
        );

        StepVerifier.create(controller.logs("Bearer bad-token", null, null, null, null, null, 200))
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof ResponseStatusException);
                    assertEquals(((ResponseStatusException) error).getStatusCode(), HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    void logsReturnsFilteredItemsForAdmin() {
        GatewayLogService logs = new GatewayLogService();
        logs.recordRequest("GET", "/api/v1/auth/login", "user-1", 15L, 200);
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        given(decoder.decode("admin-token")).willReturn(Mono.just(jwt(List.of("ADMIN"))));
        ObservabilityLogController controller = new ObservabilityLogController(
                logs,
                decoder,
                WebClient.builder(),
                "http://localhost:3100"
        );

        StepVerifier.create(controller.logs("Bearer admin-token", "login", "INFO", "RUNTIME", "auth-service", 200, 9999))
                .assertNext(body -> {
                    assertEquals(body.get("count"), 1);
                    assertEquals(body.get("limit"), 500);
                })
                .verifyComplete();
    }

    @Test
    void lokiLabelsAndServicesProxyAdminRequests() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        given(decoder.decode("admin-token")).willReturn(Mono.just(jwt(List.of("SUPER_ADMIN"))));
        List<String> requestedUrls = new ArrayList<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(jsonExchange(requestedUrls));
        ObservabilityLogController controller = new ObservabilityLogController(
                new GatewayLogService(),
                decoder,
                builder,
                "http://loki.local:3100"
        );

        StepVerifier.create(controller.lokiLabels("Bearer admin-token"))
                .assertNext(body -> assertEquals(body.get("status"), "success"))
                .verifyComplete();
        StepVerifier.create(controller.lokiServices("Bearer admin-token"))
                .assertNext(body -> assertEquals(body.get("status"), "success"))
                .verifyComplete();

        assertTrue(requestedUrls.get(0).endsWith("/loki/api/v1/labels"));
        assertTrue(requestedUrls.get(1).endsWith("/loki/api/v1/label/service/values"));
    }

    @Test
    void lokiQueryRangeAppliesDefaultsCapsAndOptionalParameters() {
        ReactiveJwtDecoder decoder = mock(ReactiveJwtDecoder.class);
        given(decoder.decode("admin-token")).willReturn(Mono.just(jwt(List.of("ADMIN"))));
        List<String> requestedUrls = new ArrayList<>();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(jsonExchange(requestedUrls));
        ObservabilityLogController controller = new ObservabilityLogController(
                new GatewayLogService(),
                decoder,
                builder,
                "http://loki.local:3100"
        );

        StepVerifier.create(controller.lokiQueryRange("Bearer admin-token", " ", 6000, "FORWARD", " ", "1720000000"))
                .assertNext(body -> assertEquals(body.get("status"), "success"))
                .verifyComplete();

        String url = requestedUrls.getFirst();
        assertTrue(url.contains("/loki/api/v1/query_range"));
        assertTrue(url.contains("limit=5000"));
        assertTrue(url.contains("direction=forward"));
        assertTrue(url.contains("end=1720000000"));
        assertFalse(url.contains("start="));
        assertTrue(url.contains("compose_project"));
    }

    private ExchangeFunction jsonExchange(List<String> requestedUrls) {
        return (ClientRequest request) -> {
            requestedUrls.add(request.url().toString());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("{\"status\":\"success\",\"data\":[]}")
                    .build());
        };
    }

    private Jwt jwt(List<String> roles) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", "user-1", "roles", roles));
    }
}
