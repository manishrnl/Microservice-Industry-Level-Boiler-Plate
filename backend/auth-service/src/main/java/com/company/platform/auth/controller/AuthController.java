package com.company.platform.auth.controller;

import com.company.platform.auth.dto.*;
import com.company.platform.auth.service.AuthService;
import com.company.platform.commons.dto.TokenDto;
import com.company.platform.commons.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public UserDto signup(@Valid @RequestBody SignupRequestDto request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public TokenDto login(@Valid @RequestBody LoginRequestDto request,
                          HttpServletRequest httpRequest,
                          HttpServletResponse response) {
        AuthTokenResponseDto tokenResponse = authService.login(request, clientMetadata(httpRequest));
        response.addHeader(HttpHeaders.SET_COOKIE, tokenResponse.getRefreshCookie());
        return tokenResponse.getToken();
    }

    @PostMapping("/refresh")
    public TokenDto refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                            HttpServletResponse response) {
        AuthTokenResponseDto tokenResponse = authService.refresh(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, tokenResponse.getRefreshCookie());
        return tokenResponse.getToken();
    }

    @PostMapping("/logout")
    public ActionResponseDto logout(HttpServletResponse response) {
        AuthCookieResponseDto logoutResponse = authService.logout();
        response.addHeader(HttpHeaders.SET_COOKIE, logoutResponse.getRefreshCookie());
        return logoutResponse.getResponse();
    }

    @GetMapping("/me")
    public AuthMeResponseDto me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt);
    }

    @PutMapping("/me/password")
    public ActionResponseDto changePassword(@Valid @RequestBody ChangePasswordRequestDto request,
                                            @AuthenticationPrincipal Jwt jwt) {
        return authService.changePassword(request, jwt);
    }

    @PostMapping("/me/suspend")
    public ActionResponseDto suspendAccount(@Valid @RequestBody AccountActionRequestDto request,
                                            @AuthenticationPrincipal Jwt jwt,
                                            HttpServletResponse response) {
        ActionResponseDto result = authService.suspendAccount(request, jwt);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie());
        return result;
    }

    @DeleteMapping("/me")
    public ActionResponseDto deleteAccount(@Valid @RequestBody AccountActionRequestDto request,
                                           @AuthenticationPrincipal Jwt jwt,
                                           HttpServletResponse response) {
        ActionResponseDto result = authService.deleteAccount(request, jwt);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie());
        return result;
    }

    @PostMapping("/verify-email")
    public ActionResponseDto verifyEmail(@Valid @RequestBody OtpVerificationRequestDto request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public ActionResponseDto resendVerification(@Valid @RequestBody EmailRequestDto request) {
        return authService.resendVerification(request);
    }

    @PostMapping("/forgot-password")
    public ActionResponseDto forgotPassword(@Valid @RequestBody EmailRequestDto request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ActionResponseDto resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/sessions")
    public SessionsResponseDto sessions(@AuthenticationPrincipal Jwt jwt) {
        return authService.sessions(jwt);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ActionResponseDto revokeSession(@PathVariable String sessionId,
                                           @AuthenticationPrincipal Jwt jwt,
                                           HttpServletResponse response) {
        ActionResponseDto result = authService.revokeSession(sessionId, jwt);
        if (Boolean.TRUE.equals(result.getRevokedCurrent())) {
            response.addHeader(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie());
        }
        return result;
    }

    @DeleteMapping("/sessions/all")
    public ActionResponseDto revokeAllSessions(@AuthenticationPrincipal Jwt jwt,
                                               HttpServletResponse response) {
        ActionResponseDto result = authService.revokeAllSessions(jwt);
        response.addHeader(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie());
        return result;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return authService.jwks();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/db-ping")
    public Map<String, Object> dbPing() {
        return authService.dbPing();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/db-stats")
    public Map<String, Object> dbStats() {
        return authService.dbStats();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/admin/ping")
    public ActionResponseDto adminPing() {
        return ActionResponseDto.builder()
                .status("ok")
                .build();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/admin/users/{userId}/unlock")
    public ActionResponseDto unlockUser(@PathVariable UUID userId) {
        return authService.unlockUser(userId);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/admin/users/{userId}/password")
    public ActionResponseDto adminChangePassword(@PathVariable UUID userId,
                                                 @Valid @RequestBody AdminPasswordUpdateRequestDto request) {
        return authService.adminChangePassword(userId, request);
    }

    private ClientRequestMetadataDto clientMetadata(HttpServletRequest request) {
        return ClientRequestMetadataDto.builder()
                .ipAddress(clientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .timeZone(request.getHeader("X-Client-Time-Zone"))
                .localTime(request.getHeader("X-Client-Local-Time"))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
