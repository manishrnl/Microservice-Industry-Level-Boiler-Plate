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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    public UserDto signup(SignupRequestDto request) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        User user = upsertLocalUser(request);
        assignUserRole(user, RoleType.USER);
        mailService.sendSignupVerification(request.getEmail(), request.getFullName());
        return userMapper.toDto(user, Set.of(RoleType.USER));
    }

    public AuthTokenResponseDto login(LoginRequestDto request, ClientRequestMetadataDto metadata) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.UnauthorizedException("Invalid email or password"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiExceptions.UnauthorizedException("Invalid email or password");
        }
        if (!user.isEmailVerified()) {
            mailService.sendSignupVerificationIfNeeded(user.getEmail(), displayName(user, user.getEmail(), user.getFullName()));
            throw new ApiExceptions.ForbiddenException("Email is not verified. Enter the OTP sent to your email.");
        }
        if (user.isAccountLocked()) {
            throw new ApiExceptions.ForbiddenException("Account is locked");
        }

        assignUserRole(user, RoleType.USER);
        return issueToken(user, request.getEmail(), request.getDeviceId(), metadata);
    }

    public AuthTokenResponseDto loginWithOAuth(OAuthLoginRequestDto request, ClientRequestMetadataDto metadata) {
        User user = upsertOAuthUser(request);
        assignUserRole(user, RoleType.USER);
        return issueToken(user, user.getEmail(), "OAuth session", metadata);
    }

    public AuthTokenResponseDto refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiExceptions.UnauthorizedException("Refresh token is missing");
        }
        UserSession session = sessionService.requireActive(refreshToken);
        User user = session.getUser();
        Set<RoleType> roles = roles(user);
        String name = displayName(user, user.getEmail(), user.getFullName());
        sessionService.touch(session.getSessionId());
        TokenDto token = TokenDto.builder()
                .accessToken(jwtTokenService.createAccessToken(user.getId(), user.getEmail(), roles, session.getSessionId(), name))
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
        String name = displayName(user, email, jwt.getClaimAsString("name"));
        return AuthMeResponseDto.builder()
                .user(userMapper.toDto(user.getId(), name, user.getEmail(), roles, user.getAvatarUrl()))
                .accessToken(jwtTokenService.createAccessToken(userId, email, roles, jwt.getClaimAsString("sessionId"), name))
                .build();
    }

    public UserDto updateProfile(ProfileUpdateRequestDto request, Jwt jwt) {
        User user = currentUser(jwt);
        user.setFullName(request.getName());
        return userMapper.toDto(userRepository.save(user), roles(jwt));
    }

    public UserDto updateAvatar(AvatarUpdateRequestDto request, Jwt jwt) {
        User user = currentUser(jwt);
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        return userMapper.toDto(userRepository.save(user), roles(jwt));
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
        mailService.resendSignupVerification(user.getEmail(), displayName(user, user.getEmail(), user.getFullName()));
        return ActionResponseDto.builder()
                .status("sent")
                .email(user.getEmail())
                .channel("email")
                .delivery("otp")
                .build();
    }

    public ActionResponseDto forgotPassword(EmailRequestDto request) {
        if (!userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ApiExceptions.ResourceNotFoundException("Account was not found");
        }
        mailService.sendPasswordReset(request.getEmail());
        return ActionResponseDto.builder()
                .status("sent")
                .channel("email")
                .delivery("otp")
                .build();
    }

    public ActionResponseDto resetPassword(ResetPasswordRequestDto request) {
        validatePasswordMatch(request.getPassword(), request.getConfirmPassword());
        if (!mailService.consumePasswordResetOtp(request.getEmail(), request.getOtp())) {
            throw new ApiExceptions.ValidationException("Invalid or expired reset OTP");
        }
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account was not found"));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        mailService.sendPasswordChanged(request.getEmail());
        return ActionResponseDto.builder()
                .status("changed")
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
        User user = User.builder()
                .email(normalizedEmail)
                .username(normalizedEmail)
                .provider("LOCAL")
                .emailVerified(false)
                .accountLocked(false)
                .failedAttempts(0)
                .build();
        user.setFullName(request.getFullName());
        user.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    private User upsertOAuthUser(OAuthLoginRequestDto request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> User.builder()
                        .email(normalizedEmail)
                        .accountLocked(false)
                        .failedAttempts(0)
                        .build());
        user.setEmail(normalizedEmail);
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setProvider(request.getProvider().toUpperCase());
        user.setProviderId(request.getProviderId());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private AuthTokenResponseDto issueToken(User user, String email, String deviceId, ClientRequestMetadataDto metadata) {
        String sessionId = UUID.randomUUID().toString();
        sessionService.create(user, sessionId, deviceId, metadata.getIpAddress(), metadata.getUserAgent());
        mailService.sendLoginNotice(email);
        TokenDto token = TokenDto.builder()
                .accessToken(jwtTokenService.createAccessToken(user.getId(), email, Set.of(RoleType.USER), sessionId, user.getFullName()))
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

    private User currentUser(Jwt jwt) {
        sessionService.requireActive(jwt.getClaimAsString("sessionId"));
        sessionService.touchIfStale(jwt.getClaimAsString("sessionId"));
        return userRepository.findById(parseUuid(jwt.getSubject()))
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
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
        return roles.isEmpty() ? Set.of(RoleType.USER) : roles;
    }

    private String displayName(User user, String email, String fallback) {
        String name = user.getFullName();
        if (name == null || name.isBlank() || name.equalsIgnoreCase(email)) {
            return fallbackName(email, fallback);
        }
        return name;
    }

    private String fallbackName(String email, String fallback) {
        if (fallback != null && !fallback.isBlank() && !fallback.equalsIgnoreCase(email)) {
            return fallback;
        }
        if (email == null || email.isBlank()) {
            return "User";
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
