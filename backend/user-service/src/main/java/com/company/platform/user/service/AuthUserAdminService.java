package com.company.platform.user.service;

import com.company.platform.user.auth.repository.AuthRoleRepository;
import com.company.platform.user.auth.repository.AuthUserRepository;
import com.company.platform.user.entity.AuthRole;
import com.company.platform.user.entity.AuthUser;
import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserProfileRepository;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthUserAdminService {
    private static final int AUTH_ACCOUNT_FETCH_LIMIT = 1000;

    private final AuthUserRepository authUsers;
    private final AuthRoleRepository authRoles;
    private final UserProfileRepository profiles;

    public AuthUserAdminService(AuthUserRepository authUsers,
                                AuthRoleRepository authRoles,
                                UserProfileRepository profiles) {
        this.authUsers = authUsers;
        this.authRoles = authRoles;
        this.profiles = profiles;
    }

    public List<UserDto> search(String query, String role) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<AuthAccount> accounts = searchAuthAccounts(role);
        Map<UUID, UserProfile> profileById = profiles.findAllById(accounts.stream().map(AuthAccount::userId).toList())
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
        return accounts.stream()
                .map(account -> toUser(account, profileById.get(account.userId())))
                .filter(user -> matches(user, normalizedQuery))
                .limit(250)
                .toList();
    }

    public UserDto get(UUID userId) {
        AuthAccount account = account(userId).orElseThrow(() -> new NoSuchElementException("User not found"));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return toUser(account, profile);
    }

    public Optional<AuthAccount> account(UUID userId) {
        return authUsers.findOneById(userId).map(this::toAccount);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public UserDto updateRoles(UUID userId, Set<RoleType> requestedRoles) {
        Set<RoleType> roles = requestedRoles == null || requestedRoles.isEmpty()
                ? Set.of(RoleType.USER)
                : requestedRoles;
        AuthUser user = authUsers.findOneById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        user.setRoles(resolveRoles(roles));
        AuthAccount account = toAccount(authUsers.save(user));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return toUser(account, profile);
    }

    @Transactional(transactionManager = "authTransactionManager")
    public void updateUsername(UUID userId, String username) {
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();
        if (normalizedUsername.isBlank()) {
            return;
        }
        if (authUsers.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, userId)) {
            throw new ApiExceptions.ConflictException("Username already registered");
        }
        authUsers.findById(userId).ifPresent(user -> user.setUsername(normalizedUsername));
    }

    private List<AuthAccount> searchAuthAccounts(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        List<AuthUser> users = normalizedRole.isBlank()
                ? authUsers.findAllByOrderByEmailAsc(PageRequest.of(0, AUTH_ACCOUNT_FETCH_LIMIT))
                : authUsers.findByRolesNameOrderByEmailAsc(normalizedRole, PageRequest.of(0, AUTH_ACCOUNT_FETCH_LIMIT));
        return users.stream()
                .map(this::toAccount)
                .toList();
    }

    private Set<AuthRole> resolveRoles(Set<RoleType> roles) {
        Set<String> roleNames = roles.stream()
                .map(RoleType::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, AuthRole> existingRoles = authRoles.findByNameIn(roleNames).stream()
                .collect(Collectors.toMap(AuthRole::getName, role -> role));
        Set<AuthRole> resolved = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            AuthRole role = existingRoles.get(roleName);
            resolved.add(role == null ? createRole(roleName) : role);
        }
        return resolved;
    }

    private AuthRole createRole(String roleName) {
        try {
            return authRoles.saveAndFlush(AuthRole.builder().name(roleName).build());
        } catch (DataIntegrityViolationException ex) {
            return authRoles.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role was created concurrently but could not be loaded", ex));
        }
    }

    private AuthAccount toAccount(AuthUser user) {
        return new AuthAccount(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getProvider(),
                user.isEmailVerified(),
                user.isAccountLocked(),
                user.getAccountStatus(),
                user.getLockedUntil(),
                user.getDeletedAt(),
                roles(user.getRoles())
        );
    }

    private UserDto toUser(AuthAccount account, UserProfile profile) {
        UserDto user = new UserDto();
        user.setUserId(account.userId());
        user.setEmail(account.email());
        user.setUsername(account.username());
        user.setName(displayName(profile == null ? null : profile.getName(), account.username(), account.email()));
        user.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        user.setRoles(account.roles().isEmpty() ? Set.of(RoleType.USER) : account.roles());
        return user;
    }

    private boolean matches(UserDto user, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return contains(user.getName(), query)
                || contains(user.getEmail(), query)
                || contains(user.getUserId().toString(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String displayName(String profileName, String username, String email) {
        if (profileName != null && !profileName.isBlank()) {
            return profileName;
        }
        if (username != null && !username.isBlank() && !username.equalsIgnoreCase(email)) {
            return username;
        }
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    private Set<RoleType> roles(Set<AuthRole> roles) {
        Set<RoleType> parsed = (roles == null ? Set.<AuthRole>of() : roles).stream()
                .map(AuthRole::getName)
                .filter(Objects::nonNull)
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        return parsed.isEmpty() ? Set.of(RoleType.USER) : parsed;
    }

    public record AuthAccount(UUID userId,
                              String email,
                              String username,
                              String provider,
                              boolean emailVerified,
                              boolean accountLocked,
                              String accountStatus,
                              LocalDateTime lockedUntil,
                              LocalDateTime deletedAt,
                              Set<RoleType> roles) {
    }
}
