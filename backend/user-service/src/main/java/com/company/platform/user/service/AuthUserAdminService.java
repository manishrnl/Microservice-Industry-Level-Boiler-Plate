package com.company.platform.user.service;

import com.company.platform.commons.api.PagedResponse;
import com.company.platform.user.auth.repository.AuthRoleRepository;
import com.company.platform.user.auth.repository.AuthUserRepository;
import com.company.platform.user.dto.AuthAccountDto;
import com.company.platform.user.entity.AuthRole;
import com.company.platform.user.entity.AuthUser;
import com.company.platform.user.mapper.UserAccountMapper;
import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserProfileRepository;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthUserAdminService {
    private static final int AUTH_ACCOUNT_FETCH_LIMIT = 1000;

    private final AuthUserRepository authUsers;
    private final AuthRoleRepository authRoles;
    private final UserProfileRepository profiles;
    private final UserAccountMapper userAccountMapper;

    public List<UserDto> search(String query, String role) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        List<AuthAccountDto> accounts = searchAuthAccounts(role);
        Map<UUID, UserProfile> profileById = profiles.findAllById(accounts.stream().map(AuthAccountDto::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile, (left, right) -> left, LinkedHashMap::new));
        return accounts.stream()
                .map(account -> userAccountMapper.toUserDto(account, profileById.get(account.getUserId())))
                .filter(user -> matches(user, normalizedQuery))
                .limit(250)
                .toList();
    }

    public PagedResponse<UserDto> searchPage(String query, String role, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        org.springframework.data.domain.Page<AuthUser> accountPage = normalizedRole.isBlank()
                ? authUsers.findByOrderByEmailAsc(pageable)
                : authUsers.findByRolesNameOrderByEmailAsc(normalizedRole, pageable);
        Map<UUID, UserProfile> profileById = profiles.findAllById(accountPage.getContent().stream().map(AuthUser::getId).toList())
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile, (left, right) -> left, LinkedHashMap::new));
        List<UserDto> content = accountPage.getContent().stream()
                .map(user -> {
                    AuthAccountDto account = userAccountMapper.toAuthAccountDto(user);
                    return userAccountMapper.toUserDto(account, profileById.get(account.getUserId()));
                })
                .filter(user -> matches(user, normalizedQuery))
                .toList();
        return new PagedResponse<>(
                content,
                accountPage.getNumber(),
                accountPage.getSize(),
                accountPage.getTotalElements(),
                accountPage.getTotalPages(),
                accountPage.isLast()
        );
    }

    public UserDto get(UUID userId) {
        AuthAccountDto account = account(userId).orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return userAccountMapper.toUserDto(account, profile);
    }

    public AuthAccountDto accountDto(UUID userId) {
        return authUsers.findOneById(userId)
                .map(userAccountMapper::toAuthAccountDto)
                .orElse(null);
    }

    public Optional<AuthAccountDto> account(UUID userId) {
        return Optional.ofNullable(accountDto(userId));
    }

    @CacheEvict(cacheNames = {"adminUsers", "users", "userAuthAccounts"}, allEntries = true)
    @Transactional(transactionManager = "authTransactionManager")
    public UserDto updateRoles(UUID userId, Set<RoleType> requestedRoles) {
        Set<RoleType> roles = userAccountMapper.defaultRoles(requestedRoles);
        AuthUser user = authUsers.findOneById(userId)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));
        user.setRoles(resolveRoles(roles));
        AuthAccountDto account = userAccountMapper.toAuthAccountDto(authUsers.save(user));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return userAccountMapper.toUserDto(account, profile);
    }

    @CacheEvict(cacheNames = {"adminUsers", "users", "userAuthAccounts"}, allEntries = true)
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

    @CacheEvict(cacheNames = {"adminUsers", "users"}, allEntries = true)
    public void evictProfileCaches() {
    }

    private List<AuthAccountDto> searchAuthAccounts(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        List<AuthUser> users = normalizedRole.isBlank()
                ? authUsers.findByOrderByEmailAsc(PageRequest.of(0, AUTH_ACCOUNT_FETCH_LIMIT)).getContent()
                : authUsers.findByRolesNameOrderByEmailAsc(normalizedRole, PageRequest.of(0, AUTH_ACCOUNT_FETCH_LIMIT)).getContent();
        return users.stream()
                .map(userAccountMapper::toAuthAccountDto)
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
}
