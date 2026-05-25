package com.company.platform.auth.controller;

import com.company.platform.auth.config.OAuthProviderProperties;
import com.company.platform.auth.dto.AuthTokenResponseDto;
import com.company.platform.auth.dto.ClientRequestMetadataDto;
import com.company.platform.auth.dto.OAuthLoginRequestDto;
import com.company.platform.auth.service.AuthService;
import com.company.platform.commons.dto.TokenDto;
import com.company.platform.commons.exception.ApiExceptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class OAuthControllerAuthorizeTest {

    @Test
    void authorizeBuildsProviderRedirectWithDefaultProviderUrisAndState() {
        OAuthProviderProperties properties = properties();
        OAuthProviderProperties.Provider google = new OAuthProviderProperties.Provider();
        google.setClientId("client-id");
        google.setClientSecret("secret");
        properties.setProviders(Map.of("google", google));
        OAuthController controller = new OAuthController(properties, mock(AuthService.class), new ObjectMapper());

        var response = controller.authorize("google", "Asia/Kolkata", "2026-05-24 18:00");

        assertEquals(response.getStatusCode(), HttpStatus.FOUND);
        assertTrue(String.valueOf(response.getHeaders().getLocation().toString()).contains(String.valueOf("accounts.google.com")));
        assertTrue(String.valueOf(response.getHeaders().getLocation().toString()).contains(String.valueOf("client_id=client-id")));
        assertTrue(String.valueOf(response.getHeaders().getLocation().toString()).contains(String.valueOf("redirect_uri=")));
        assertTrue(String.valueOf(response.getHeaders().getLocation().toString()).contains(String.valueOf("state=")));
    }

    @Test
    void authorizeRejectsUnsupportedOrUnconfiguredProviders() {
        OAuthController controller = new OAuthController(properties(), mock(AuthService.class), new ObjectMapper());

        expectThrows(
                ApiExceptions.ServiceUnavailableException.class,
                () -> controller.authorize("unknown", null, null)
        );
    }

    @Test
    void authorizeRejectsMissingRedirectBaseAndInvalidProviderUrls() {
        OAuthProviderProperties missingRedirect = properties();
        missingRedirect.setRedirectBaseUrl("${REDIRECT_BASE_URL}");
        OAuthController missingRedirectController = new OAuthController(missingRedirect, mock(AuthService.class), new ObjectMapper());
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> missingRedirectController.authorize("google", null, null));

        OAuthProviderProperties invalidUrl = properties();
        OAuthProviderProperties.Provider google = new OAuthProviderProperties.Provider();
        google.setClientId("client-id");
        google.setClientSecret("secret");
        google.setAuthorizationUri("://bad");
        invalidUrl.setProviders(Map.of("google", google));
        OAuthController invalidUrlController = new OAuthController(invalidUrl, mock(AuthService.class), new ObjectMapper());

        expectThrows(ApiExceptions.ValidationException.class,
                () -> invalidUrlController.authorize("google", null, null));
    }

    @Test
    void providerSpecificUserInfoExtractionHandlesGithubLinkedInAndValidationFailures() throws Exception {
        OAuthController controller = new OAuthController(properties(), mock(AuthService.class), new ObjectMapper());
        ObjectMapper mapper = new ObjectMapper();

        Object github = ReflectionTestUtils.invokeMethod(controller, "extractOAuthUserInfo", "github",
                mapper.readTree("{\"email\":\"git@example.com\",\"name\":\"Git User\",\"avatar_url\":\"/git.png\",\"id\":42,\"login\":\"gituser\"}"),
                "provider-access");
        Object linkedin = ReflectionTestUtils.invokeMethod(controller, "extractOAuthUserInfo", "linkedin",
                mapper.readTree("{\"elements\":[{\"emailAddress\":\"link@example.com\"}],\"localizedFirstName\":\"Link\",\"localizedLastName\":\"User\",\"id\":\"li-1\"}"),
                "provider-access");

        assertTrue(String.valueOf(github).contains("git@example.com"));
        assertTrue(String.valueOf(github).contains("gituser"));
        assertTrue(String.valueOf(linkedin).contains("link@example.com"));
        assertTrue(String.valueOf(linkedin).contains("Link User"));
        assertTrue(String.valueOf(linkedin).contains("linkedin-li-1"));
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "extractOAuthUserInfo", "google",
                        mapper.readTree("{\"name\":\"No Email\"}"), "provider-access"));
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "extractOAuthUserInfo", "unknown",
                        mapper.readTree("{}"), "provider-access"));
    }

    @Test
    void githubUserInfoFallsBackToPrimaryVerifiedEmailEndpoint() throws Exception {
        OAuthController controller = new OAuthController(properties(), mock(AuthService.class), new ObjectMapper());
        HttpClient client = mock(HttpClient.class);
        given(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .willReturn(httpResponse(200, "[{\"email\":\"primary@example.com\",\"primary\":true,\"verified\":true}]"));
        ReflectionTestUtils.setField(controller, "httpClient", client);

        Object github = ReflectionTestUtils.invokeMethod(controller, "extractOAuthUserInfo", "github",
                new ObjectMapper().readTree("{\"name\":\"Git User\",\"avatar_url\":\"/git.png\",\"id\":42,\"login\":\"gituser\"}"),
                "provider-access");

        assertTrue(String.valueOf(github).contains("primary@example.com"));
        verify(client).send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void oauthHttpHelpersRejectBadTokenAndUserInfoResponses() throws Exception {
        OAuthProviderProperties.Provider provider = new OAuthProviderProperties.Provider();
        provider.setClientId("client-id");
        provider.setClientSecret("secret");
        provider.setTokenUri("http://provider.local/token");
        provider.setUserInfoUri("http://provider.local/userinfo");
        OAuthController controller = new OAuthController(properties(), mock(AuthService.class), new ObjectMapper());
        HttpClient client = mock(HttpClient.class);
        ReflectionTestUtils.setField(controller, "httpClient", client);

        given(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .willReturn(httpResponse(500, "{}"));
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "exchangeCodeForAccessToken", provider, "google", "code"));

        given(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .willReturn(httpResponse(200, "{}"));
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "exchangeCodeForAccessToken", provider, "google", "code"));

        given(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .willReturn(httpResponse(503, "{}"));
        expectThrows(ApiExceptions.ServiceUnavailableException.class,
                () -> ReflectionTestUtils.invokeMethod(controller, "fetchUserInfo", provider, "access"));
    }

    @Test
    void callbackExchangesCodeFetchesProfileAndRedirectsWithAccessToken() throws Exception {
        AtomicReference<String> tokenBody = new AtomicReference<>();
        AtomicReference<String> userInfoAuthorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, "{\"access_token\":\"provider-access\"}");
        });
        server.createContext("/userinfo", exchange -> {
            userInfoAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, "{\"email\":\"oauth@example.com\",\"name\":\"OAuth User\",\"picture\":\"/avatar.png\",\"sub\":\"sub-1\",\"given_name\":\"oauth\"}");
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            OAuthProviderProperties properties = properties();
            OAuthProviderProperties.Provider google = new OAuthProviderProperties.Provider();
            google.setClientId("client-id");
            google.setClientSecret("client-secret");
            google.setTokenUri(baseUrl + "/token");
            google.setUserInfoUri(baseUrl + "/userinfo");
            properties.setProviders(Map.of("google", google));
            AuthService authService = mock(AuthService.class);
            given(authService.loginWithOAuth(any(OAuthLoginRequestDto.class), any(ClientRequestMetadataDto.class)))
                    .willReturn(AuthTokenResponseDto.builder()
                            .token(TokenDto.builder().accessToken("platform-access").build())
                            .refreshCookie("refresh_token=refresh-1")
                            .build());
            OAuthController controller = new OAuthController(properties, authService, new ObjectMapper());
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("127.0.0.2");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
            request.addHeader("User-Agent", "Chrome");
            MockHttpServletResponse response = new MockHttpServletResponse();
            String state = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    "{\"nonce\":\"n1\",\"timeZone\":\"Asia/Kolkata\",\"localTime\":\"2026-05-25 10:00\"}".getBytes(StandardCharsets.UTF_8));

            var redirect = controller.callback("google", "auth-code", state, request, response);

            assertEquals(redirect.getStatusCode(), HttpStatus.FOUND);
            assertTrue(redirect.getHeaders().getLocation().toString().contains("access_token=platform-access"));
            assertEquals(response.getHeader("Set-Cookie"), "refresh_token=refresh-1");
            assertTrue(tokenBody.get().contains("code=auth-code"));
            assertTrue(tokenBody.get().contains("client_secret=client-secret"));
            assertEquals(userInfoAuthorization.get(), "Bearer provider-access");
            ArgumentCaptor<OAuthLoginRequestDto> loginCaptor = ArgumentCaptor.forClass(OAuthLoginRequestDto.class);
            ArgumentCaptor<ClientRequestMetadataDto> metadataCaptor = ArgumentCaptor.forClass(ClientRequestMetadataDto.class);
            verify(authService).loginWithOAuth(loginCaptor.capture(), metadataCaptor.capture());
            assertEquals(loginCaptor.getValue().getEmail(), "oauth@example.com");
            assertEquals(loginCaptor.getValue().getUsername(), "oauth");
            assertEquals(loginCaptor.getValue().getProviderId(), "sub-1");
            assertEquals(metadataCaptor.getValue().getIpAddress(), "10.0.0.1");
            assertEquals(metadataCaptor.getValue().getTimeZone(), "Asia/Kolkata");
            assertEquals(metadataCaptor.getValue().getLocalTime(), "2026-05-25 10:00");
        } finally {
            server.stop(0);
        }
    }

    private OAuthProviderProperties properties() {
        OAuthProviderProperties properties = new OAuthProviderProperties();
        properties.setRedirectBaseUrl("http://localhost:8081");
        properties.setFrontendRedirectUrl("http://localhost:5173");
        return properties;
    }

    private void respond(HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private HttpResponse<String> httpResponse(int statusCode, String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return statusCode;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public java.util.Optional<HttpResponse<String>> previousResponse() {
                return java.util.Optional.empty();
            }

            @Override
            public java.net.http.HttpHeaders headers() {
                return java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public java.util.Optional<javax.net.ssl.SSLSession> sslSession() {
                return java.util.Optional.empty();
            }

            @Override
            public java.net.URI uri() {
                return java.net.URI.create("http://provider.local");
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }
}
