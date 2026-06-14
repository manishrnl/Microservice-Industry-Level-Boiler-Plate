package com.company.platform.auth.service;

import com.company.platform.auth.dto.*;
import com.company.platform.auth.entity.*;
import com.company.platform.auth.mapper.AuthUserMapper;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.auth.email.AuthMailService;
import com.company.platform.auth.security.JwtTokenService;
import com.company.platform.auth.security.RefreshTokenCookieFactory;
import com.company.platform.auth.security.RsaKeyService;
import com.company.platform.commons.dto.TokenDto;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 900;
    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";
    private static final String ACCOUNT_STATUS_SUSPENDED = "SUSPENDED";
    private static final String ACCOUNT_STATUS_DELETED = "DELETED";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthSessionService sessionService;
    private final AuthUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthMailService mailService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final RsaKeyService rsaKeyService;
    private final JdbcTemplate jdbcTemplate;
    private final AuthNotificationService notificationService;
    private final AuthDemoDataService demoDataService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.bootstrap-super-admin-email:manishrajrnl@gmail.com}")
    private String bootstrapSuperAdminEmail;

    public UserDto signup(SignupRequestDto request) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        User user = upsertLocalUser(request);
        assignUserRole(user, RoleType.USER);
        ensureBaselineRole(user);
        mailService.sendSignupVerification(request.getEmail(), request.getFullName());
        demoDataService.provision(user, request.getFullName(), request.getAvatarUrl());
        return userMapper.toDto(user, roles(user));
    }

    public AuthTokenResponseDto login(LoginRequestDto request, ClientRequestMetadataDto metadata) {
        String identifier = loginIdentifier(request);
        User user = findByLoginIdentifier(identifier)
                .orElseThrow(() -> new ApiExceptions.UnauthorizedException("Invalid email, username, or password"));
        if (loginAttemptService.unlockIfExpired(user.getId())) {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
        }
        normalizeAccountStatus(user);
        if (ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new ApiExceptions.ForbiddenException("Account is deleted");
        }
        if (user.isAccountLocked()) {
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            LoginAttemptService.LoginFailureResult failure = loginAttemptService.recordFailure(user.getId());
            int maxAttempts = loginAttemptService.maxFailedAttempts();
            if (failure.locked()) {
                throw new ApiExceptions.ForbiddenException("Account locked after " + maxAttempts + " failed login attempts. Contact a super admin to unlock this account.");
            }
            int remainingAttempts = Math.max(0, maxAttempts - failure.failedAttempts());
            throw new ApiExceptions.UnauthorizedException("Invalid email, username, or password. Failed attempt " + failure.failedAttempts() + " of " + maxAttempts + ". " + remainingAttempts + " attempt" + (remainingAttempts == 1 ? "" : "s") + " remaining.");
        }
        if (!user.isEmailVerified()) {
            mailService.sendSignupVerificationIfNeeded(user.getEmail(), displayName(user, user.getEmail(), user.getUsername()));
            throw new ApiExceptions.ForbiddenException("Email is not verified. Enter the OTP sent to your email.");
        }
        loginAttemptService.recordSuccess(user.getId());
        user.setFailedAttempts(0);
        ensureBaselineRole(user);
        return issueToken(user, user.getEmail(), request.getDeviceId(), metadata);
    }

    public AuthTokenResponseDto loginWithOAuth(OAuthLoginRequestDto request, ClientRequestMetadataDto metadata) {
        User user = upsertOAuthUser(request);
        normalizeAccountStatus(user);
        if (user.isAccountLocked() || ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }
        ensureBaselineRole(user);
        demoDataService.provision(user);
        return issueToken(user, user.getEmail(), "OAuth session", metadata);
    }

    public AuthTokenResponseDto refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiExceptions.UnauthorizedException("Refresh token is missing");
        }
        UserSession session = sessionService.requireActive(refreshToken);
        User user = session.getUser();
        normalizeAccountStatus(user);
        if (user.isAccountLocked() || ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            sessionService.revoke(user, session.getSessionId());
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }
        ensureBaselineRole(user);
        Set<RoleType> roles = roles(user);
        String name = displayName(user, user.getEmail(), user.getUsername());
        sessionService.touch(session.getSessionId());
        TokenDto token = TokenDto.builder()
                .accessToken(jwtTokenService.createAccessToken(user.getId(), user.getEmail(), roles, session.getSessionId(), name, user.getUsername()))
                .tokenType("Bearer")
                .expiresInSeconds(ACCESS_TOKEN_EXPIRES_IN_SECONDS)
                .build();
        return AuthTokenResponseDto.builder()
                .token(token)
                .refreshCookie(cookieFactory.create(session.getSessionId()).toString())
                .build();
    }

    public AuthCookieResponseDto logout() {
        return AuthCookieResponseDto.builder()
                .response(ActionResponseDto.builder().status("logged_out").build())
                .refreshCookie(cookieFactory.clear().toString())
                .build();
    }

    public String clearRefreshCookie() {
        return cookieFactory.clear().toString();
    }

    public AuthMeResponseDto me(Jwt jwt) {
        UUID userId = parseUuid(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        Set<RoleType> roles = roles(jwt);
        sessionService.requireActive(jwt.getClaimAsString("sessionId"));
        sessionService.touchIfStale(jwt.getClaimAsString("sessionId"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        normalizeAccountStatus(user);
        if (user.isAccountLocked() || ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }
        ensureBaselineRole(user);
        roles = roles(user);
        String name = displayName(user, email, jwt.getClaimAsString("name"));
        return AuthMeResponseDto.builder()
                .user(userMapper.toDto(user.getId(), name, user.getEmail(), roles, null))
                .accessToken(jwtTokenService.createAccessToken(userId, email, roles, jwt.getClaimAsString("sessionId"), name, user.getUsername()))
                .build();
    }

    public ActionResponseDto changePassword(ChangePasswordRequestDto request, Jwt jwt) {
        validatePasswordMatch(request.getNewPassword(), request.getConfirmPassword());
        User user = currentUser(jwt);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiExceptions.UnauthorizedException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        mailService.sendPasswordChanged(user.getEmail());
        return ActionResponseDto.builder()
                .status("password_changed")
                .build();
    }

    public ActionResponseDto suspendAccount(AccountActionRequestDto request, Jwt jwt) {
        requireConfirmation(request.getConfirmation(), "SUSPEND");
        User user = currentUser(jwt);
        int days = request.getDays() == null ? 7 : Math.max(1, Math.min(90, request.getDays()));
        user.setAccountLocked(true);
        user.setAccountStatus(ACCOUNT_STATUS_SUSPENDED);
        user.setLockedUntil(LocalDateTime.now().plusDays(days));
        userRepository.save(user);
        sessionService.revokeAll(user);
        return ActionResponseDto.builder()
                .status("account_suspended")
                .revoked(true)
                .revokedCurrent(true)
                .build();
    }

    public ActionResponseDto deleteAccount(AccountActionRequestDto request, Jwt jwt) {
        requireConfirmation(request.getConfirmation(), "DELETE");
        User user = currentUser(jwt);
        user.setAccountLocked(true);
        user.setAccountStatus(ACCOUNT_STATUS_DELETED);
        user.setDeletedAt(LocalDateTime.now());
        user.setLockedUntil(null);
        userRepository.save(user);
        sessionService.revokeAll(user);
        return ActionResponseDto.builder()
                .status("account_deleted")
                .revoked(true)
                .revokedCurrent(true)
                .build();
    }

    public ActionResponseDto verifyEmail(OtpVerificationRequestDto request) {
        if (!mailService.verifySignupOtp(request.getEmail(), request.getOtp())) {
            throw new ApiExceptions.ValidationException("Invalid or expired verification OTP");
        }
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account was not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        mailService.sendEmailVerified(user.getEmail());
        return ActionResponseDto.builder()
                .status("verified")
                .email(request.getEmail())
                .build();
    }

    public ActionResponseDto resendVerification(EmailRequestDto request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account was not found"));
        if (user.isEmailVerified()) {
            return ActionResponseDto.builder()
                    .status("already_verified")
                    .email(user.getEmail())
                    .build();
        }
        mailService.resendSignupVerification(user.getEmail(), displayName(user, user.getEmail(), user.getUsername()));
        return ActionResponseDto.builder()
                .status("sent")
                .email(user.getEmail())
                .channel("email")
                .delivery("otp")
                .build();
    }

    public ActionResponseDto forgotPassword(EmailRequestDto request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account was not found"));
        if (loginAttemptService.unlockIfExpired(user.getId())) {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
        }
        normalizeAccountStatus(user);
        if (user.isAccountLocked()) {
            throw new ApiExceptions.ForbiddenException("Account is locked. Contact a super admin to unlock this account before resetting the password.");
        }
        mailService.sendPasswordReset(user.getEmail());
        return ActionResponseDto.builder()
                .status("sent")
                .channel("email")
                .delivery("otp")
                .build();
    }

    public ActionResponseDto resetPassword(ResetPasswordRequestDto request) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account was not found"));
        if (loginAttemptService.unlockIfExpired(user.getId())) {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            user.setLockedUntil(null);
        }
        normalizeAccountStatus(user);
        if (user.isAccountLocked()) {
            throw new ApiExceptions.ForbiddenException("Account is locked. Contact a super admin to unlock this account before resetting the password.");
        }
        if (!mailService.consumePasswordResetOtp(request.getEmail(), request.getOtp())) {
            throw new ApiExceptions.ValidationException("Invalid or expired reset OTP");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        loginAttemptService.recordSuccess(user.getId());
        user.setFailedAttempts(0);
        if (ACCOUNT_STATUS_ACTIVE.equals(user.getAccountStatus())) {
            user.setAccountLocked(false);
            user.setLockedUntil(null);
        }
        userRepository.save(user);
        mailService.sendPasswordChanged(request.getEmail());
        return ActionResponseDto.builder()
                .status("changed")
                .build();
    }

    public ActionResponseDto unlockUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        normalizeAccountStatus(user);
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        if (ACCOUNT_STATUS_SUSPENDED.equals(user.getAccountStatus())) {
            user.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        }
        userRepository.save(user);
        return ActionResponseDto.builder()
                .status("account_unlocked")
                .build();
    }

    public ActionResponseDto adminChangePassword(UUID userId, AdminPasswordUpdateRequestDto request) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        if (ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new ApiExceptions.ForbiddenException("Deleted accounts cannot receive a new password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFailedAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        if (ACCOUNT_STATUS_SUSPENDED.equals(user.getAccountStatus())) {
            user.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        }
        userRepository.save(user);
        sessionService.revokeAll(user);
        mailService.sendPasswordChanged(user.getEmail());
        return ActionResponseDto.builder()
                .status("password_changed")
                .revoked(true)
                .build();
    }

    public SessionsResponseDto sessions(Jwt jwt) {
        UUID userId = parseUuid(jwt.getSubject());
        String currentSessionId = jwt.getClaimAsString("sessionId");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        return SessionsResponseDto.builder().content(sessionService.list(user, currentSessionId)).build();
    }

    public ActionResponseDto revokeSession(String sessionId, Jwt jwt) {
        User user = userRepository.findById(parseUuid(jwt.getSubject()))
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        sessionService.revoke(user, sessionId);
        return ActionResponseDto.builder()
                .revoked(true)
                .revokedCurrent(sessionId.equals(jwt.getClaimAsString("sessionId")))
                .build();
    }

    public ActionResponseDto revokeAllSessions(Jwt jwt) {
        User user = userRepository.findById(parseUuid(jwt.getSubject()))
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        sessionService.revokeAll(user);
        return ActionResponseDto.builder()
                .revoked(true)
                .revokedCurrent(true)
                .build();
    }

    @Cacheable(cacheNames = "authJwks", key = "'current'")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> jwks() {
        return rsaKeyService.jwks();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> dbPing() {
        long startedAt = System.nanoTime();
        Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        return Map.of(
                "status", "ok",
                "database", "auth_db",
                "probe", "select_1",
                "result", result == null ? 0 : result,
                "elapsedMs", elapsedMs
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Map<String, Object> dbStats() {
        long startedAt = System.nanoTime();
        Long users = jdbcTemplate.queryForObject("select count(*) from users", Long.class);
        Long sessions = jdbcTemplate.queryForObject("select count(*) from user_sessions", Long.class);
        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
        return Map.of(
                "status", "ok",
                "database", "auth_db",
                "probe", "table_counts",
                "elapsedMs", elapsedMs,
                "users", users == null ? 0 : users,
                "sessions", sessions == null ? 0 : sessions
        );
    }

    private User upsertLocalUser(SignupRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiExceptions.ConflictException("Email already registered");
        }
        String username = usernameForSignup(request, normalizedEmail);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiExceptions.ConflictException("Username already registered");
        }
        User user = User.builder()
                .email(normalizedEmail)
                .username(username)
                .provider("LOCAL")
                .accountStatus(ACCOUNT_STATUS_ACTIVE)
                .emailVerified(false)
                .accountLocked(false)
                .failedAttempts(0)
                .build();
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    private User upsertOAuthUser(OAuthLoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .accountLocked(false)
                        .accountStatus(ACCOUNT_STATUS_ACTIVE)
                        .failedAttempts(0)
                        .build());
        user.setEmail(normalizedEmail);
        user.setUsername(usernameForOAuth(request, normalizedEmail));
        user.setProvider(request.getProvider().toUpperCase());
        user.setProviderId(request.getProviderId());
        user.setEmailVerified(true);
        user.setAccountStatus(user.getAccountStatus() == null ? ACCOUNT_STATUS_ACTIVE : user.getAccountStatus());
        return userRepository.save(user);
    }

    private AuthTokenResponseDto issueToken(User user, String email, String deviceId, ClientRequestMetadataDto metadata) {
        String sessionId = UUID.randomUUID().toString();
        boolean suspiciousLogin = sessionService.isSuspiciousLogin(user, deviceId, metadata.getIpAddress(), metadata.getUserAgent());
        sessionService.create(user, sessionId, deviceId, metadata.getIpAddress(), metadata.getUserAgent());
        mailService.sendLoginNotice(email);
        if (suspiciousLogin) {
            mailService.sendSuspiciousLoginWarning(email, metadata.getIpAddress(), metadata.getUserAgent());
        }
        notificationService.loginDetected(user, sessionId, metadata);
        demoDataService.provision(user);
        Set<RoleType> roles = roles(user);
        String name = displayName(user, email, user.getUsername());
        TokenDto token = TokenDto.builder()
                .accessToken(jwtTokenService.createAccessToken(user.getId(), email, roles, sessionId, name, user.getUsername()))
                .tokenType("Bearer")
                .expiresInSeconds(ACCESS_TOKEN_EXPIRES_IN_SECONDS)
                .build();
        return AuthTokenResponseDto.builder()
                .token(token)
                .refreshCookie(cookieFactory.create(sessionId).toString())
                .build();
    }

    private void assignUserRole(User user, RoleType roleType) {
        Role role = roleRepository.findByName(roleType.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleType.name()).build()));
        if (!userRoleRepository.existsByUserAndRole(user, role)) {
            userRoleRepository.save(UserRole.builder()
                    .id(UserRoleId.builder()
                            .userId(user.getId())
                            .roleId(role.getId())
                            .build())
                    .user(user)
                    .role(role)
                    .build());
        }
    }

    private void ensureBaselineRole(User user) {
        if (isBootstrapSuperAdmin(user.getEmail())) {
            assignUserRole(user, RoleType.SUPER_ADMIN);
            return;
        }
        if (roles(user).isEmpty()) {
            assignUserRole(user, RoleType.USER);
        }
    }

    private boolean isBootstrapSuperAdmin(String email) {
        return bootstrapSuperAdminEmail != null
                && !bootstrapSuperAdminEmail.isBlank()
                && normalizeEmail(bootstrapSuperAdminEmail).equals(normalizeEmail(email));
    }

    private User currentUser(Jwt jwt) {
        sessionService.requireActive(jwt.getClaimAsString("sessionId"));
        sessionService.touchIfStale(jwt.getClaimAsString("sessionId"));
        User user = userRepository.findById(parseUuid(jwt.getSubject()))
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        normalizeAccountStatus(user);
        if (user.isAccountLocked() || ACCOUNT_STATUS_DELETED.equals(user.getAccountStatus())) {
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }
        return user;
    }

    private void normalizeAccountStatus(User user) {
        if (user.getAccountStatus() == null || user.getAccountStatus().isBlank()) {
            user.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        }
        if (ACCOUNT_STATUS_SUSPENDED.equals(user.getAccountStatus())
                && user.getLockedUntil() != null
                && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setAccountLocked(false);
            user.setLockedUntil(null);
            user.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
            userRepository.save(user);
        }
    }

    private void requireConfirmation(String actual, String expected) {
        if (actual == null || !expected.equals(actual.trim())) {
            throw new ApiExceptions.ValidationException("Type " + expected + " to confirm this account action");
        }
    }

    private void validatePasswordMatch(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new ApiExceptions.ValidationException("Password and confirm password must match");
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException ex) {
            return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private Set<RoleType> roles(Jwt jwt) {
        List<String> roleClaims = jwt.getClaimAsStringList("roles");
        if (roleClaims == null || roleClaims.isEmpty()) {
            return Set.of(RoleType.USER);
        }
        return roleClaims.stream()
                .map(RoleType::valueOf)
                .collect(Collectors.toSet());
    }

    private Set<RoleType> roles(User user) {
        Set<RoleType> roles = userRoleRepository.findByUser(user).stream()
                .map(userRole -> userRole.getRole().getName())
                .map(RoleType::valueOf)
                .collect(Collectors.toSet());
        return roles;
    }

    private String displayName(User user, String email, String fallback) {
        if (fallback != null && !fallback.isBlank() && !fallback.equalsIgnoreCase(email)) {
            return fallback;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank() && !user.getUsername().equalsIgnoreCase(email)) {
            return user.getUsername();
        }
        if (email == null || email.isBlank()) {
            return "User";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String loginIdentifier(LoginRequestDto request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            identifier = request.getEmail();
        }
        if (identifier == null || identifier.isBlank()) {
            throw new ApiExceptions.ValidationException("Email or username is required");
        }
        return identifier.trim();
    }

    private java.util.Optional<User> findByLoginIdentifier(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(normalizeEmail(identifier));
        }
        return userRepository.findByUsernameIgnoreCase(normalizeUsername(identifier));
    }

    private String usernameForSignup(SignupRequestDto request, String normalizedEmail) {
        String username = normalizeUsername(request.getUsername());
        return username.isBlank() ? normalizedEmail : username;
    }

    private String usernameForOAuth(OAuthLoginRequestDto request, String normalizedEmail) {
        String username = normalizeUsername(request.getUsername());
        return username.isBlank() ? normalizedEmail : username;
    }
}
