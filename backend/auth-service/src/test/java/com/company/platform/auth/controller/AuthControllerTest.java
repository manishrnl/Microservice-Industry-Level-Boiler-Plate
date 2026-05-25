package com.company.platform.auth.controller;

import com.company.platform.auth.dto.ActionResponseDto;
import com.company.platform.auth.dto.AccountActionRequestDto;
import com.company.platform.auth.dto.AuthCookieResponseDto;
import com.company.platform.auth.dto.AuthMeResponseDto;
import com.company.platform.auth.dto.AuthTokenResponseDto;
import com.company.platform.auth.dto.ChangePasswordRequestDto;
import com.company.platform.auth.dto.ClientRequestMetadataDto;
import com.company.platform.auth.dto.EmailRequestDto;
import com.company.platform.auth.dto.LoginRequestDto;
import com.company.platform.auth.dto.OtpVerificationRequestDto;
import com.company.platform.auth.dto.ResetPasswordRequestDto;
import com.company.platform.auth.dto.SessionsResponseDto;
import com.company.platform.auth.service.AuthService;
import com.company.platform.commons.dto.TokenDto;
import com.company.platform.commons.dto.UserDto;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private final AuthController controller = new AuthController(service);

    @Test
    void loginAddsRefreshCookieAndBuildsClientMetadataFromHeaders() {
        LoginRequestDto request = LoginRequestDto.builder().identifier("admin").password("Password@123").build();
        TokenDto token = TokenDto.builder().accessToken("access").tokenType("Bearer").expiresInSeconds(900).build();
        given(service.login(org.mockito.ArgumentMatchers.eq(request), any(ClientRequestMetadataDto.class)))
                .willReturn(AuthTokenResponseDto.builder().token(token).refreshCookie("refresh_token=refresh; Path=/api/v1/auth/refresh; HttpOnly; SameSite=Lax").build());
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRemoteAddr("10.0.0.1");
        servletRequest.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        servletRequest.addHeader("User-Agent", "Chrome");
        servletRequest.addHeader("X-Client-Time-Zone", "Asia/Kolkata");
        servletRequest.addHeader("X-Client-Local-Time", "2026-05-24 18:00");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertEquals(controller.login(request, servletRequest, response), token);

        ArgumentCaptor<ClientRequestMetadataDto> metadata = ArgumentCaptor.forClass(ClientRequestMetadataDto.class);
        verify(service).login(org.mockito.ArgumentMatchers.eq(request), metadata.capture());
        assertEquals(metadata.getValue().getIpAddress(), "203.0.113.10");
        assertEquals(metadata.getValue().getUserAgent(), "Chrome");
        assertTrue(String.valueOf(response.getHeader(HttpHeaders.SET_COOKIE)).contains(String.valueOf("refresh_token=refresh")));
    }

    @Test
    void refreshLogoutAndSessionRevocationWriteCookiesWhenRequired() {
        TokenDto token = TokenDto.builder().accessToken("access").build();
        Jwt jwt = jwt(UUID.randomUUID().toString(), "session-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(service.refresh("refresh")).willReturn(AuthTokenResponseDto.builder().token(token).refreshCookie("refresh_token=new; Path=/api/v1/auth/refresh; HttpOnly; SameSite=Lax").build());
        given(service.logout()).willReturn(AuthCookieResponseDto.builder()
                .response(ActionResponseDto.builder().status("logged_out").build())
                .refreshCookie("refresh_token=; Path=/api/v1/auth/refresh; Max-Age=0; HttpOnly; SameSite=Lax")
                .build());
        given(service.revokeSession("session-1", jwt)).willReturn(ActionResponseDto.builder().revoked(true).revokedCurrent(true).build());
        given(service.clearRefreshCookie()).willReturn("refresh_token=; Path=/api/v1/auth/refresh; Max-Age=0; HttpOnly; SameSite=Lax");

        assertEquals(controller.refresh("refresh", response), token);
        assertEquals(controller.logout(response).getStatus(), "logged_out");
        assertTrue(controller.revokeSession("session-1", jwt, response).getRevokedCurrent());

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(cookies.stream().anyMatch(value -> value.contains("refresh_token=new")));
        assertTrue(cookies.stream().anyMatch(value -> value.contains("Max-Age=0")));
    }

    @Test
    void simpleEndpointsDelegateToAuthService() {
        Jwt jwt = jwt(UUID.randomUUID().toString(), "session-1");
        given(service.sessions(jwt)).willReturn(SessionsResponseDto.builder().content(List.of()).build());
        given(service.jwks()).willReturn(Map.of("keys", List.of()));
        given(service.dbPing()).willReturn(Map.of("status", "ok"));
        given(service.dbStats()).willReturn(Map.of("users", 1L));

        assertTrue(controller.sessions(jwt).getContent().isEmpty());
        assertTrue(controller.jwks().containsKey("keys"));
        assertEquals(controller.dbPing().get("status"), "ok");
        assertEquals(controller.dbStats().get("users"), 1L);
        assertEquals(controller.adminPing().getStatus(), "ok");
    }

    @Test
    void profilePasswordAndAccountActionEndpointsDelegateAndClearCookies() {
        Jwt jwt = jwt(UUID.randomUUID().toString(), "session-1");
        ChangePasswordRequestDto password = new ChangePasswordRequestDto();
        AccountActionRequestDto suspend = new AccountActionRequestDto();
        AccountActionRequestDto delete = new AccountActionRequestDto();
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(service.me(jwt)).willReturn(AuthMeResponseDto.builder().accessToken("access").build());
        given(service.changePassword(password, jwt)).willReturn(ActionResponseDto.builder().status("password_changed").build());
        given(service.suspendAccount(suspend, jwt)).willReturn(ActionResponseDto.builder().status("account_suspended").build());
        given(service.deleteAccount(delete, jwt)).willReturn(ActionResponseDto.builder().status("account_deleted").build());
        given(service.clearRefreshCookie()).willReturn("refresh_token=; Max-Age=0");

        assertEquals(controller.me(jwt).getAccessToken(), "access");
        assertEquals(controller.changePassword(password, jwt).getStatus(), "password_changed");
        assertEquals(controller.suspendAccount(suspend, jwt, response).getStatus(), "account_suspended");
        assertEquals(controller.deleteAccount(delete, jwt, response).getStatus(), "account_deleted");
        assertEquals(response.getHeaders(HttpHeaders.SET_COOKIE).size(), 2);
    }

    @Test
    void verificationRecoveryAndSessionEndpointsDelegateToAuthService() {
        Jwt jwt = jwt(UUID.randomUUID().toString(), "session-1");
        OtpVerificationRequestDto otp = new OtpVerificationRequestDto("u@example.com", "111111");
        EmailRequestDto email = new EmailRequestDto("u@example.com");
        ResetPasswordRequestDto reset = new ResetPasswordRequestDto("u@example.com", "111111", "Password@123", "Password@123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(service.verifyEmail(otp)).willReturn(ActionResponseDto.builder().status("verified").build());
        given(service.resendVerification(email)).willReturn(ActionResponseDto.builder().status("sent").build());
        given(service.forgotPassword(email)).willReturn(ActionResponseDto.builder().status("sent").build());
        given(service.resetPassword(reset)).willReturn(ActionResponseDto.builder().status("changed").build());
        given(service.revokeAllSessions(jwt)).willReturn(ActionResponseDto.builder().revoked(true).revokedCurrent(true).build());
        given(service.clearRefreshCookie()).willReturn("refresh_token=; Max-Age=0");

        assertEquals(controller.verifyEmail(otp).getStatus(), "verified");
        assertEquals(controller.resendVerification(email).getStatus(), "sent");
        assertEquals(controller.forgotPassword(email).getStatus(), "sent");
        assertEquals(controller.resetPassword(reset).getStatus(), "changed");
        assertTrue(controller.revokeAllSessions(jwt, response).getRevoked());
        assertTrue(response.getHeader(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
    }

    @Test
    void signupDelegates() {
        var signup = com.company.platform.auth.dto.SignupRequestDto.builder()
                .email("user@example.com")
                .username("user")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("User")
                .build();
        UserDto dto = UserDto.builder().userId(UUID.randomUUID()).email("user@example.com").name("User").build();
        given(service.signup(signup)).willReturn(dto);

        assertEquals(controller.signup(signup), dto);
    }

    private Jwt jwt(String subject, String sessionId) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", subject, "sessionId", sessionId));
    }
}
