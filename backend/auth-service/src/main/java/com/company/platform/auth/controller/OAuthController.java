package com.company.platform.auth.controller;

import com.company.platform.auth.config.OAuthProviderProperties;
import com.company.platform.auth.dto.AuthTokenResponseDto;
import com.company.platform.auth.dto.ClientRequestMetadataDto;
import com.company.platform.auth.dto.OAuthLoginRequestDto;
import com.company.platform.auth.service.AuthService;
import com.company.platform.commons.exception.ApiExceptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuthController {
    private static final Map<String, ProviderDefaults> PROVIDER_DEFAULTS = Map.of(
            "google", new ProviderDefaults(
                    "https://accounts.google.com/o/oauth2/v2/auth",
                    "https://oauth2.googleapis.com/token",
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    "openid,profile,email"),
            "github", new ProviderDefaults(
                    "https://github.com/login/oauth/authorize",
                    "https://github.com/login/oauth/access_token",
                    "https://api.github.com/user",
                    "read:user,user:email"),
            "linkedin", new ProviderDefaults(
                    "https://www.linkedin.com/oauth/v2/authorization",
                    "https://www.linkedin.com/oauth/v2/accessToken",
                    "https://api.linkedin.com/v2/userinfo",
                    "openid,profile,email")
    );

    private final OAuthProviderProperties properties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OAuthController(OAuthProviderProperties properties,
                           AuthService authService,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.authService = authService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    @GetMapping("/authorize/{provider}")
    public ResponseEntity<Void> authorize(@PathVariable("provider") String provider,
                                          @RequestParam(value = "timeZone", required = false) String timeZone,
                                          @RequestParam(value = "localTime", required = false) String localTime) {
        OAuthProviderProperties.Provider settings = provider(provider);
        URI redirectUri;
        try {
            redirectUri = UriComponentsBuilder.fromUriString(settings.getAuthorizationUri())
                    .queryParam("response_type", "code")
                    .queryParam("client_id", settings.getClientId())
                    .queryParam("redirect_uri", callbackUrl(provider))
                    .queryParam("scope", scopes(settings))
                    .queryParam("state", encodeState(new OAuthClientContext(UUID.randomUUID().toString(), timeZone, localTime)))
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new ApiExceptions.ValidationException("OAuth provider has an invalid authorization URL: " + provider);
        }
        return ResponseEntity.status(302).location(redirectUri).build();
    }

    @GetMapping("/callback/{provider}")
    public ResponseEntity<Void> callback(@PathVariable("provider") String provider,
                                         @RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        OAuthProviderProperties.Provider settings = provider(provider);
        OAuthUserInfo userInfo = fetchOAuthUserInfo(settings, provider, code);
        AuthTokenResponseDto tokenResponse = authService.loginWithOAuth(OAuthLoginRequestDto.builder()
                .email(userInfo.email())
                .username(userInfo.username())
                .fullName(userInfo.name())
                .avatarUrl(userInfo.avatarUrl())
                .provider(provider)
                .providerId(userInfo.providerId())
                .build(), clientMetadata(request, decodeState(state)));
        String accessToken = tokenResponse.getToken().getAccessToken();
        response.addHeader(HttpHeaders.SET_COOKIE, tokenResponse.getRefreshCookie());
        URI frontendRedirect = UriComponentsBuilder.fromUriString(frontendRedirectUrl())
                .path("/oauth/callback")
                .fragment("access_token=" + accessToken + "&provider=" + provider)
                .build()
                .toUri();
        return ResponseEntity.status(302).location(frontendRedirect).build();
    }

    private OAuthProviderProperties.Provider provider(String provider) {
        if (!isConfiguredValue(properties.getRedirectBaseUrl())) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth redirect base URL is not configured");
        }
        String normalizedProvider = provider.toLowerCase();
        ProviderDefaults defaults = PROVIDER_DEFAULTS.get(normalizedProvider);
        if (defaults == null) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth provider is not supported: " + provider);
        }
        Map<String, OAuthProviderProperties.Provider> configuredProviders = properties.getProviders() == null ? Map.of() : properties.getProviders();
        OAuthProviderProperties.Provider settings = configuredProviders.get(provider.toLowerCase());
        if (settings == null || !isConfiguredValue(settings.getClientId())) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth provider is not configured: " + provider);
        }
        if (!isConfiguredValue(settings.getAuthorizationUri())) {
            settings.setAuthorizationUri(defaults.authorizationUri());
        }
        if (!isConfiguredValue(settings.getTokenUri())) {
            settings.setTokenUri(defaults.tokenUri());
        }
        if (!isConfiguredValue(settings.getUserInfoUri())) {
            settings.setUserInfoUri(defaults.userInfoUri());
        }
        if (!isConfiguredValue(settings.getScopes())) {
            settings.setScopes(defaults.scopes());
        }
        return settings;
    }

    private String scopes(OAuthProviderProperties.Provider settings) {
        return Arrays.stream(settings.getScopes().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    private OAuthUserInfo fetchOAuthUserInfo(OAuthProviderProperties.Provider settings, String provider, String code) {
        String accessToken = exchangeCodeForAccessToken(settings, provider, code);
        JsonNode profile = fetchUserInfo(settings, accessToken);
        return extractOAuthUserInfo(provider, profile, accessToken);
    }

    private String exchangeCodeForAccessToken(OAuthProviderProperties.Provider settings, String provider, String code) {
        if (!isConfiguredValue(settings.getTokenUri())) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth token endpoint is not configured for: " + provider);
        }
        String body = formEncode(Map.of(
                "client_id", settings.getClientId(),
                "client_secret", settings.getClientSecret(),
                "code", code,
                "redirect_uri", callbackUrl(provider),
                "grant_type", "authorization_code"
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getTokenUri()))
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiExceptions.ServiceUnavailableException("OAuth token exchange failed for provider: " + provider);
            }
            JsonNode tokenResponse = objectMapper.readTree(response.body());
            String accessToken = textValue(tokenResponse, "access_token");
            if (!StringUtils.hasText(accessToken)) {
                throw new ApiExceptions.ServiceUnavailableException("OAuth provider did not return an access token for: " + provider);
            }
            return accessToken;
        } catch (IOException ex) {
            throw new ApiExceptions.ServiceUnavailableException("Unable to exchange OAuth " +
                    "code for provider: " + provider + ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiExceptions.ServiceUnavailableException("OAuth token exchange " +
                    "interrupted for provider: " + provider + ex);
        }
    }

    private JsonNode fetchUserInfo(OAuthProviderProperties.Provider settings, String accessToken) {
        if (!isConfiguredValue(settings.getUserInfoUri())) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth user info endpoint is not configured");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(settings.getUserInfoUri()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiExceptions.ServiceUnavailableException("OAuth user info fetch failed with status " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new ApiExceptions.ServiceUnavailableException("Unable to fetch OAuth user " +
                    "info" + ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiExceptions.ServiceUnavailableException("OAuth user info fetch " +
                    "interrupted" + ex);
        }
    }

    private OAuthUserInfo extractOAuthUserInfo(String provider, JsonNode profile, String accessToken) {
        String lowerProvider = provider.toLowerCase();
        String email;
        String name;
        String avatarUrl = null;
        String providerId;
        String username;

        switch (lowerProvider) {
            case "google":
                email = textValue(profile, "email");
                name = textValue(profile, "name");
                avatarUrl = textValue(profile, "picture");
                providerId = textValue(profile, "sub");
                username = textValue(profile, "given_name");
                break;
            case "github":
                email = textValue(profile, "email");
                if (!StringUtils.hasText(email)) {
                    email = fetchGithubEmail(accessToken);
                }
                name = textValue(profile, "name");
                avatarUrl = textValue(profile, "avatar_url");
                providerId = textValue(profile, "id");
                username = textValue(profile, "login");
                break;
            case "linkedin":
                email = textValue(profile, "email");
                if (!StringUtils.hasText(email) && profile.has("elements")) {
                    email = profile.path("elements").findValuesAsText("emailAddress").stream()
                            .filter(StringUtils::hasText)
                            .findFirst()
                            .orElse(null);
                }
                String firstName = textValue(profile, "localizedFirstName");
                String lastName = textValue(profile, "localizedLastName");
                name = StringUtils.hasText(firstName) ? firstName + (StringUtils.hasText(lastName) ? " " + lastName : "") : textValue(profile, "name");
                providerId = textValue(profile, "id");
                username = textValue(profile, "vanityName");
                break;
            default:
                throw new ApiExceptions.ServiceUnavailableException("Unsupported OAuth provider: " + provider);
        }

        if (!StringUtils.hasText(email)) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth provider did not return an email for: " + provider);
        }
        if (!StringUtils.hasText(name)) {
            name = email.substring(0, email.indexOf('@'));
        }
        if (!StringUtils.hasText(username)) {
            username = provider.toLowerCase() + "-" + (StringUtils.hasText(providerId) ? providerId : UUID.randomUUID().toString());
        }
        if (!StringUtils.hasText(providerId)) {
            providerId = email;
        }
        return new OAuthUserInfo(email, name, avatarUrl, providerId, username);
    }

    private String fetchGithubEmail(String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/user/emails"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode emails = objectMapper.readTree(response.body());
            if (emails.isArray()) {
                Optional<String> primary = Optional.empty();
                Optional<String> verified = Optional.empty();
                for (JsonNode emailNode : emails) {
                    String email = textValue(emailNode, "email");
                    if (!StringUtils.hasText(email)) {
                        continue;
                    }
                    if (emailNode.path("primary").asBoolean(false) && emailNode.path("verified").asBoolean(false)) {
                        return email;
                    }
                    if (primary.isEmpty()) {
                        primary = Optional.of(email);
                    }
                }
                return primary.orElse(null);
            }
            return null;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String formEncode(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String textValue(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private String callbackUrl(String provider) {
        return properties.getRedirectBaseUrl() + "/api/v1/auth/oauth2/callback/" + provider;
    }

    private String frontendRedirectUrl() {
        if (!isConfiguredValue(properties.getFrontendRedirectUrl())) {
            throw new ApiExceptions.ServiceUnavailableException("OAuth frontend redirect URL is not configured");
        }
        return properties.getFrontendRedirectUrl();
    }

    private boolean isConfiguredValue(String value) {
        return StringUtils.hasText(value) && !value.contains("${");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private ClientRequestMetadataDto clientMetadata(HttpServletRequest request) {
        return clientMetadata(request, null);
    }

    private ClientRequestMetadataDto clientMetadata(HttpServletRequest request, OAuthClientContext context) {
        return ClientRequestMetadataDto.builder()
                .ipAddress(clientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .timeZone(firstText(request.getHeader("X-Client-Time-Zone"), context == null ? null : context.timeZone()))
                .localTime(firstText(request.getHeader("X-Client-Local-Time"), context == null ? null : context.localTime()))
                .build();
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String encodeState(OAuthClientContext context) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsString(context).getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException ex) {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(("{\"nonce\":\"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8));
        }
    }

    private OAuthClientContext decodeState(String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(state);
            JsonNode node = objectMapper.readTree(decoded);
            return new OAuthClientContext(textValue(node, "nonce"), textValue(node, "timeZone"), textValue(node, "localTime"));
        } catch (IllegalArgumentException | IOException ex) {
            return null;
        }
    }

    private record ProviderDefaults(String authorizationUri, String tokenUri,
                                    String userInfoUri, String scopes) {
    }

    private record OAuthClientContext(String nonce, String timeZone, String localTime) {
    }

    private record OAuthUserInfo(String email, String name, String avatarUrl,
                                 String providerId, String username) {
    }
}
