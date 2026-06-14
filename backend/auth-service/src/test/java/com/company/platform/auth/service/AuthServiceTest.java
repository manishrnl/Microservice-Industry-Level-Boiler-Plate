package com.company.platform.auth.service;

import com.company.platform.auth.dto.*;
import com.company.platform.auth.email.AuthMailService;
import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.UserRoleId;
import com.company.platform.auth.entity.UserSession;
import com.company.platform.auth.mapper.AuthUserMapper;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.auth.security.JwtTokenService;
import com.company.platform.auth.security.RefreshTokenCookieFactory;
import com.company.platform.auth.security.RsaKeyService;
import com.company.platform.commons.dto.TokenDto;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthServiceTest {
    @Mock
    private UserRepository users;
    @Mock
    private RoleRepository roles;
    @Mock
    private UserRoleRepository userRoles;
    @Mock
    private AuthSessionService sessions;
    @Mock
    private AuthUserMapper mapper;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private AuthMailService mail;
    @Mock
    private JwtTokenService tokens;
    @Mock
    private RsaKeyService keys;
    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private AuthNotificationService notifications;
    @Mock
    private AuthDemoDataService demoData;
    @Mock
    private LoginAttemptService loginAttempts;

    private AuthService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AuthService(users, roles, userRoles, sessions, mapper, encoder, mail, tokens,
                new RefreshTokenCookieFactory(false, "Lax", 7), keys, jdbc, notifications, demoData, loginAttempts);
        ReflectionTestUtils.setField(service, "bootstrapSuperAdminEmail", "super@example.com");
    }

    @Test
    void signupValidatesPasswordsAndCreatesLocalUserWithUserRole() {
        SignupRequestDto request = SignupRequestDto.builder()
                .email("User@Example.com")
                .username(" User ")
                .password("Password@123")
                .confirmPassword("Password@123")
                .fullName("User Example")
                .avatarUrl("/a.png")
                .build();
        Role userRole = role(RoleType.USER);
        UserDto expected = UserDto.builder().email("user@example.com").name("User Example").roles(Set.of(RoleType.USER)).build();
        given(users.existsByEmailIgnoreCase("user@example.com")).willReturn(false);
        given(users.existsByUsernameIgnoreCase("user")).willReturn(false);
        given(encoder.encode("Password@123")).willReturn("hash");
        given(users.save(any(User.class))).willAnswer(invocation -> withId(invocation.getArgument(0)));
        given(roles.findByName(RoleType.USER.name())).willReturn(Optional.of(userRole));
        given(userRoles.existsByUserAndRole(any(User.class), org.mockito.ArgumentMatchers.eq(userRole))).willReturn(false);
        given(userRoles.findByUser(any(User.class))).willReturn(List.of(userRole(userRole)));
        given(mapper.toDto(any(User.class), org.mockito.ArgumentMatchers.eq(Set.of(RoleType.USER)))).willReturn(expected);

        assertEquals(service.signup(request), expected);

        verify(mail).sendSignupVerification("User@Example.com", "User Example");
        verify(demoData).provision(any(User.class), org.mockito.ArgumentMatchers.eq("User Example"), org.mockito.ArgumentMatchers.eq("/a.png"));
    }

    @Test
    void signupRejectsPasswordMismatchAndDuplicateIdentifiers() {
        SignupRequestDto mismatch = SignupRequestDto.builder().email("a@example.com").password("one").confirmPassword("two").build();
        expectThrows(ApiExceptions.ValidationException.class, () -> service.signup(mismatch));

        SignupRequestDto duplicateEmail = SignupRequestDto.builder()
                .email("a@example.com").username("a").password("Password@123").confirmPassword("Password@123").build();
        given(users.existsByEmailIgnoreCase("a@example.com")).willReturn(true);
        expectThrows(ApiExceptions.ConflictException.class, () -> service.signup(duplicateEmail));
    }

    @Test
    void loginRequiresIdentifierAndRejectsBadCredentialsOrUnverifiedEmail() {
        expectThrows(
                ApiExceptions.ValidationException.class,
                () -> service.login(LoginRequestDto.builder().password("p").build(), metadata())
        );

        given(users.findByEmailIgnoreCase("u@example.com")).willReturn(Optional.empty());
        expectThrows(
                ApiExceptions.UnauthorizedException.class,
                () -> service.login(LoginRequestDto.builder().email("u@example.com").password("bad").build(), metadata())
        );

        User user = user("u@example.com", "user", false, false, "ACTIVE");
        given(users.findByEmailIgnoreCase("u@example.com")).willReturn(Optional.of(user));
        given(loginAttempts.recordFailure(user.getId()))
                .willReturn(new LoginAttemptService.LoginFailureResult(1, false, null));
        given(encoder.matches("bad", user.getPasswordHash())).willReturn(false);
        expectThrows(
                ApiExceptions.UnauthorizedException.class,
                () -> service.login(LoginRequestDto.builder().email("u@example.com").password("bad").build(), metadata())
        );

        given(encoder.matches("Password@123", user.getPasswordHash())).willReturn(true);
        expectThrows(
                ApiExceptions.ForbiddenException.class,
                () -> service.login(LoginRequestDto.builder().email("u@example.com").password("Password@123").build(), metadata())
        );
        verify(mail).sendSignupVerificationIfNeeded("u@example.com", "user");
    }

    @Test
    void loginIssuesTokensAndSecurityNotificationsForVerifiedUser() {
        User user = user("u@example.com", "user", true, false, "");
        Role userRole = role(RoleType.USER);
        given(users.findByUsernameIgnoreCase("user")).willReturn(Optional.of(user));
        given(encoder.matches("Password@123", user.getPasswordHash())).willReturn(true);
        given(userRoles.findByUser(user)).willReturn(List.of(), List.of(userRole(userRole)));
        given(roles.findByName(RoleType.USER.name())).willReturn(Optional.of(userRole));
        given(sessions.isSuspiciousLogin(user, "desktop", "127.0.0.1", "Chrome")).willReturn(true);
        given(tokens.createAccessToken(eq(user.getId()), eq("u@example.com"), eq(Set.of(RoleType.USER)), anyString(), eq("user"), eq("user")))
                .willReturn("access");

        AuthTokenResponseDto response = service.login(LoginRequestDto.builder()
                .identifier(" user ")
                .password("Password@123")
                .deviceId("desktop")
                .build(), metadata());

        assertEquals(response.getToken().getAccessToken(), "access");
        assertTrue(String.valueOf(response.getRefreshCookie()).contains(String.valueOf("refresh_token=")));
        assertEquals(user.getAccountStatus(), "ACTIVE");
        verify(mail).sendLoginNotice("u@example.com");
        verify(mail).sendSuspiciousLoginWarning("u@example.com", "127.0.0.1", "Chrome");
        verify(notifications).loginDetected(eq(user), anyString(), any(ClientRequestMetadataDto.class));
        verify(demoData).provision(user);
    }

    @Test
    void oauthLoginCreatesUserNormalizesProviderAndIssuesSession() {
        Role userRole = role(RoleType.USER);
        given(users.findByEmailIgnoreCase("oauth@example.com")).willReturn(Optional.empty());
        given(users.save(any(User.class))).willAnswer(invocation -> withId(invocation.getArgument(0)));
        given(userRoles.findByUser(any(User.class))).willReturn(List.of(), List.of(userRole(userRole)));
        given(roles.findByName(RoleType.USER.name())).willReturn(Optional.of(userRole));
        given(tokens.createAccessToken(any(UUID.class), eq("oauth@example.com"), eq(Set.of(RoleType.USER)), anyString(), eq("oauth"), eq("oauth")))
                .willReturn("oauth-access");

        AuthTokenResponseDto response = service.loginWithOAuth(OAuthLoginRequestDto.builder()
                .email(" OAuth@Example.com ")
                .username(" OAuth ")
                .fullName("OAuth User")
                .provider("google")
                .providerId("google-1")
                .build(), metadata());

        assertEquals(response.getToken().getAccessToken(), "oauth-access");
        verify(users).save(org.mockito.ArgumentMatchers.argThat(user ->
                "oauth@example.com".equals(user.getEmail())
                        && "oauth".equals(user.getUsername())
                        && "GOOGLE".equals(user.getProvider())
                        && user.isEmailVerified()));
        verify(demoData, org.mockito.Mockito.times(2)).provision(any(User.class));
    }

    @Test
    void refreshIssuesNewAccessTokenForActiveSession() {
        User user = user("u@example.com", "user", true, false, "ACTIVE");
        UserSession session = UserSession.builder().user(user).sessionId("refresh-1").build();
        given(sessions.requireActive("refresh-1")).willReturn(session);
        given(userRoles.findByUser(user)).willReturn(List.of(userRole(role(RoleType.ADMIN))));
        given(tokens.createAccessToken(user.getId(), user.getEmail(), Set.of(RoleType.ADMIN), "refresh-1", "user", "user"))
                .willReturn("access");

        AuthTokenResponseDto response = service.refresh("refresh-1");

        assertEquals(response.getToken().getAccessToken(), "access");
        assertEquals(response.getToken().getTokenType(), "Bearer");
        assertTrue(String.valueOf(response.getRefreshCookie()).contains(String.valueOf("refresh_token=refresh-1")));
        verify(sessions).touch("refresh-1");
    }

    @Test
    void refreshRejectsMissingTokenAndRevokesLockedAccountSession() {
        expectThrows(ApiExceptions.UnauthorizedException.class, () -> service.refresh(" "));

        User user = user("u@example.com", "user", true, true, "ACTIVE");
        UserSession session = UserSession.builder().user(user).sessionId("refresh-1").build();
        given(sessions.requireActive("refresh-1")).willReturn(session);

        expectThrows(ApiExceptions.ForbiddenException.class, () -> service.refresh("refresh-1"));
        verify(sessions).revoke(user, "refresh-1");
    }

    @Test
    void meRefreshesUserFromDatabaseAndReissuesAccessToken() {
        UUID userId = UUID.nameUUIDFromBytes("external-subject".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        User user = user("u@example.com", "profile", true, false, "ACTIVE");
        ReflectionTestUtils.setField(user, "id", userId);
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", "external-subject", "email", "u@example.com", "name", "", "roles", List.of("USER"), "sessionId", "session-1"));
        UserDto dto = UserDto.builder().userId(userId).name("profile").email("u@example.com").roles(Set.of(RoleType.USER)).build();
        given(users.findById(userId)).willReturn(Optional.of(user));
        given(userRoles.findByUser(user)).willReturn(List.of(userRole(role(RoleType.USER))));
        given(mapper.toDto(userId, "profile", "u@example.com", Set.of(RoleType.USER), null)).willReturn(dto);
        given(tokens.createAccessToken(userId, "u@example.com", Set.of(RoleType.USER), "session-1", "profile", "profile"))
                .willReturn("new-access");

        AuthMeResponseDto response = service.me(jwt);

        assertEquals(response.getUser(), dto);
        assertEquals(response.getAccessToken(), "new-access");
        verify(sessions).requireActive("session-1");
        verify(sessions).touchIfStale("session-1");
    }

    @Test
    void accountActionsRequireConfirmationAndRevokeSessions() {
        User user = user("u@example.com", "user", true, false, "ACTIVE");
        User deleteUser = user("delete@example.com", "delete", true, false, "ACTIVE");
        Jwt jwt = jwt(user.getId().toString(), "session-1", List.of("USER"));
        Jwt deleteJwt = jwt(deleteUser.getId().toString(), "session-2", List.of("USER"));
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(users.findById(deleteUser.getId())).willReturn(Optional.of(deleteUser));
        AccountActionRequestDto suspend = new AccountActionRequestDto();
        suspend.setConfirmation("SUSPEND");
        suspend.setDays(100);
        AccountActionRequestDto delete = new AccountActionRequestDto();
        delete.setConfirmation("DELETE");

        assertEquals(service.suspendAccount(suspend, jwt).getStatus(), "account_suspended");
        assertTrue(user.getLockedUntil().isBefore(LocalDateTime.now().plusDays(91)));
        assertEquals(service.deleteAccount(delete, deleteJwt).getStatus(), "account_deleted");
        assertNotNull(deleteUser.getDeletedAt());
        verify(sessions).revokeAll(user);
        verify(sessions).revokeAll(deleteUser);
    }

    @Test
    void changePasswordRefreshesExpiredSuspensionAndSendsPasswordChangedMail() {
        User user = user("u@example.com", "user", true, true, "SUSPENDED");
        user.setLockedUntil(LocalDateTime.now().minusMinutes(5));
        Jwt jwt = jwt(user.getId().toString(), "session-1", List.of("USER"));
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setCurrentPassword("OldPassword@123");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(encoder.matches("OldPassword@123", user.getPasswordHash())).willReturn(true);
        given(encoder.encode("NewPassword@123")).willReturn("new-hash");

        ActionResponseDto response = service.changePassword(request, jwt);

        assertEquals(response.getStatus(), "password_changed");
        assertFalse(user.isAccountLocked());
        assertEquals(user.getAccountStatus(), "ACTIVE");
        assertEquals(user.getPasswordHash(), "new-hash");
        verify(mail).sendPasswordChanged("u@example.com");
    }

    @Test
    void changePasswordRejectsMismatchMissingUserAndBadCurrentPassword() {
        ChangePasswordRequestDto mismatch = new ChangePasswordRequestDto();
        mismatch.setCurrentPassword("OldPassword@123");
        mismatch.setNewPassword("NewPassword@123");
        mismatch.setConfirmPassword("Different@123");
        expectThrows(ApiExceptions.ValidationException.class, () -> service.changePassword(mismatch, jwt(UUID.randomUUID().toString(), "s1", List.of("USER"))));

        User user = user("u@example.com", "user", true, false, "ACTIVE");
        Jwt jwt = jwt(user.getId().toString(), "session-1", List.of("USER"));
        ChangePasswordRequestDto request = new ChangePasswordRequestDto();
        request.setCurrentPassword("wrong");
        request.setNewPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(encoder.matches("wrong", user.getPasswordHash())).willReturn(false);

        expectThrows(ApiExceptions.UnauthorizedException.class, () -> service.changePassword(request, jwt));
    }

    @Test
    void superAdminPasswordChangeUnlocksRestrictedAccountAndRejectsDeletedAccount() {
        User user = user("locked@example.com", "locked", true, true, "SUSPENDED");
        user.setFailedAttempts(10);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        AdminPasswordUpdateRequestDto request = new AdminPasswordUpdateRequestDto();
        request.setPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(encoder.encode("NewPassword@123")).willReturn("new-hash");

        ActionResponseDto response = service.adminChangePassword(user.getId(), request);

        assertEquals(response.getStatus(), "password_changed");
        assertEquals(user.getPasswordHash(), "new-hash");
        assertEquals(user.getFailedAttempts(), 0);
        assertFalse(user.isAccountLocked());
        assertNull(user.getLockedUntil());
        assertEquals(user.getAccountStatus(), "ACTIVE");
        verify(sessions).revokeAll(user);
        verify(mail).sendPasswordChanged("locked@example.com");

        User deleted = user("deleted@example.com", "deleted", true, true, "DELETED");
        given(users.findById(deleted.getId())).willReturn(Optional.of(deleted));
        expectThrows(ApiExceptions.ForbiddenException.class, () -> service.adminChangePassword(deleted.getId(), request));
    }

    @Test
    void otpPasswordAndSessionManagementFlowsDelegateCorrectly() {
        User user = user("u@example.com", "user", true, false, "ACTIVE");
        given(mail.verifySignupOtp("u@example.com", "111111")).willReturn(true);
        given(users.findByEmailIgnoreCase("u@example.com")).willReturn(Optional.of(user));
        assertEquals(service.verifyEmail(new OtpVerificationRequestDto("u@example.com", "111111")).getStatus(), "verified");

        user.setEmailVerified(true);
        assertEquals(service.resendVerification(new EmailRequestDto("u@example.com")).getStatus(), "already_verified");

        given(users.existsByEmailIgnoreCase("missing@example.com")).willReturn(false);
        expectThrows(
                ApiExceptions.ResourceNotFoundException.class,
                () -> service.forgotPassword(new EmailRequestDto("missing@example.com"))
        );

        given(mail.consumePasswordResetOtp("u@example.com", "222222")).willReturn(true);
        given(encoder.encode("NewPassword@123")).willReturn("new-hash");
        assertEquals(service.resetPassword(new ResetPasswordRequestDto("u@example.com", "222222", "NewPassword@123", "NewPassword@123")).getStatus(), "changed");
        assertEquals(user.getPasswordHash(), "new-hash");
    }

    @Test
    void resendVerificationForgotPasswordAndResetRejectionPathsUseMailServiceContracts() {
        User unverified = user("u@example.com", "user", false, false, "ACTIVE");
        given(users.findByEmailIgnoreCase("u@example.com")).willReturn(Optional.of(unverified));

        ActionResponseDto resend = service.resendVerification(new EmailRequestDto("u@example.com"));
        assertEquals(resend.getStatus(), "sent");
        assertEquals(resend.getChannel(), "email");
        verify(mail).resendSignupVerification("u@example.com", "user");

        given(users.existsByEmailIgnoreCase("u@example.com")).willReturn(true);
        ActionResponseDto forgot = service.forgotPassword(new EmailRequestDto("u@example.com"));
        assertEquals(forgot.getStatus(), "sent");
        verify(mail).sendPasswordReset("u@example.com");

        given(mail.consumePasswordResetOtp("u@example.com", "000000")).willReturn(false);
        expectThrows(
                ApiExceptions.ValidationException.class,
                () -> service.resetPassword(new ResetPasswordRequestDto("u@example.com", "000000", "NewPassword@123", "NewPassword@123"))
        );
    }

    @Test
    void sessionsAndRevokeOperationsUseJwtSubject() {
        User user = user("u@example.com", "user", true, false, "ACTIVE");
        Jwt jwt = jwt(user.getId().toString(), "current", List.of("USER"));
        given(users.findById(user.getId())).willReturn(Optional.of(user));
        given(sessions.list(user, "current")).willReturn(List.of(SessionInfoDto.builder().sessionId("current").build()));

        assertEquals(service.sessions(jwt).getContent().size(), 1);
        assertFalse(service.revokeSession("other", jwt).getRevokedCurrent());
        assertTrue(service.revokeAllSessions(jwt).getRevokedCurrent());
    }

    @Test
    void logoutJwksAndDatabaseProbeEndpointsReturnExpectedPayloads() {
        given(keys.jwks()).willReturn(Map.of("keys", List.of()));
        given(jdbc.queryForObject("select 1", Integer.class)).willReturn(1);
        given(jdbc.queryForObject("select count(*) from users", Long.class)).willReturn(2L);
        given(jdbc.queryForObject("select count(*) from user_sessions", Long.class)).willReturn(3L);

        assertEquals(service.logout().getResponse().getStatus(), "logged_out");
        assertTrue(String.valueOf(service.clearRefreshCookie()).contains(String.valueOf("Max-Age=0")));
        assertTrue(service.jwks().containsKey("keys"));
        assertEquals(service.dbPing().get("result"), 1);
        assertEquals(service.dbStats().get("users"), 2L);
        assertEquals(service.dbStats().get("sessions"), 3L);
    }

    private User user(String email, String username, boolean verified, boolean locked, String status) {
        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash("hash")
                .provider("LOCAL")
                .emailVerified(verified)
                .accountLocked(locked)
                .accountStatus(status)
                .build();
        return withId(user);
    }

    private User withId(User user) {
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Role role(RoleType type) {
        return Role.builder().id(UUID.randomUUID()).name(type.name()).build();
    }

    private UserRole userRole(Role role) {
        return UserRole.builder()
                .id(new UserRoleId(UUID.randomUUID(), role.getId()))
                .role(role)
                .build();
    }

    private ClientRequestMetadataDto metadata() {
        return ClientRequestMetadataDto.builder().ipAddress("127.0.0.1").userAgent("Chrome").build();
    }

    private Jwt jwt(String subject, String sessionId, List<String> roles) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", subject, "email", "u@example.com", "name", "user", "roles", roles, "sessionId", sessionId));
    }
}
