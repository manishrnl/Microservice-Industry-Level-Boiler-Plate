package com.company.platform.user.service;

import com.company.platform.commons.config.ModelMapperConfig;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.commons.exception.ApiExceptions;
import com.company.platform.user.auth.repository.AuthRoleRepository;
import com.company.platform.user.auth.repository.AuthUserRepository;
import com.company.platform.user.entity.AuthRole;
import com.company.platform.user.entity.AuthUser;
import com.company.platform.user.mapper.UserAccountMapper;
import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserProfileRepository;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthUserAdminServiceTest {
    @Mock
    private AuthUserRepository authUsers;
    @Mock
    private AuthRoleRepository authRoles;
    @Mock
    private UserProfileRepository profiles;

    private UserAccountMapper mapper;
    private AuthUserAdminService service;

    @BeforeMethod
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new UserAccountMapper(new ModelMapperConfig().modelMapper());
        ReflectionTestUtils.invokeMethod(mapper, "configureMappings");
        service = new AuthUserAdminService(authUsers, authRoles, profiles, mapper);
    }

    @Test
    void searchFetchesAuthAccountsByRoleAndFiltersAgainstMappedUserFields() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "admin@example.com", "admin", Set.of("ADMIN"));
        UserProfile profile = UserProfile.builder().userId(userId).name("MANISH").build();
        given(authUsers.findByRolesNameOrderByEmailAsc(org.mockito.ArgumentMatchers.eq("ADMIN"), any(Pageable.class))).willReturn(List.of(user));
        given(profiles.findAllById(List.of(userId))).willReturn(List.of(profile));

        var rows = service.search("mani", " admin ");
        assertEquals(rows.size(), 1);
        assertEquals(rows.getFirst().getName(), "MANISH");
        assertEquals(rows.getFirst().getRoles(), Set.of(RoleType.ADMIN));
    }

    @Test
    void searchWithoutRoleReturnsAllMatchingAccountsAndHandlesMissingProfiles() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "person@example.com", "person", Set.of("USER"));
        given(authUsers.findAllByOrderByEmailAsc(any(Pageable.class))).willReturn(List.of(user));
        given(profiles.findAllById(List.of(userId))).willReturn(List.of());

        var rows = service.search("person@example.com", " ");

        assertEquals(rows.size(), 1);
        assertEquals(rows.getFirst().getEmail(), "person@example.com");
    }

    @Test
    void getAndAccountReturnMappedAuthUserOrThrow() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "user@example.com", "user", Set.of("USER"));
        given(authUsers.findOneById(userId)).willReturn(Optional.of(user), Optional.empty());
        given(profiles.findById(userId)).willReturn(Optional.empty());

        assertEquals(service.get(userId).getEmail(), "user@example.com");
        expectThrows(NoSuchElementException.class, () -> service.get(userId));
    }

    @Test
    void updateRolesResolvesExistingAndCreatesMissingRoles() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "user@example.com", "user", Set.of("USER"));
        AuthRole admin = AuthRole.builder().id(UUID.randomUUID()).name("ADMIN").build();
        given(authUsers.findOneById(userId)).willReturn(Optional.of(user));
        given(authRoles.findByNameIn(anyCollection())).willReturn(List.of(admin));
        given(authRoles.saveAndFlush(any(AuthRole.class))).willAnswer(invocation -> {
            AuthRole role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return role;
        });
        given(authUsers.save(user)).willReturn(user);
        given(profiles.findById(userId)).willReturn(Optional.empty());

        var dto = service.updateRoles(userId, Set.of(RoleType.ADMIN, RoleType.SUPER_ADMIN));

        assertTrue(String.valueOf(dto.getRoles()).contains(String.valueOf(RoleType.ADMIN)));
        assertTrue(String.valueOf(dto.getRoles()).contains(String.valueOf(RoleType.SUPER_ADMIN)));
    }

    @Test
    void updateRolesReloadsRoleCreatedConcurrentlyAfterIntegrityViolation() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "user@example.com", "user", Set.of("USER"));
        AuthRole admin = AuthRole.builder().id(UUID.randomUUID()).name("ADMIN").build();
        given(authUsers.findOneById(userId)).willReturn(Optional.of(user));
        given(authRoles.findByNameIn(anyCollection())).willReturn(List.of());
        given(authRoles.saveAndFlush(any(AuthRole.class))).willThrow(new DataIntegrityViolationException("duplicate"));
        given(authRoles.findByName("ADMIN")).willReturn(Optional.of(admin));
        given(authUsers.save(user)).willReturn(user);
        given(profiles.findById(userId)).willReturn(Optional.empty());

        assertEquals(service.updateRoles(userId, Set.of(RoleType.ADMIN)).getRoles(), Set.of(RoleType.ADMIN));
    }

    @Test
    void updateRolesThrowsWhenUserOrConcurrentRoleReloadIsMissing() {
        UUID userId = UUID.randomUUID();
        given(authUsers.findOneById(userId)).willReturn(Optional.empty());
        expectThrows(NoSuchElementException.class, () -> service.updateRoles(userId, Set.of(RoleType.USER)));

        AuthUser user = authUser(userId, "user@example.com", "user", Set.of("USER"));
        given(authUsers.findOneById(userId)).willReturn(Optional.of(user));
        given(authRoles.findByNameIn(anyCollection())).willReturn(List.of());
        given(authRoles.saveAndFlush(any(AuthRole.class))).willThrow(new DataIntegrityViolationException("duplicate"));
        given(authRoles.findByName("ADMIN")).willReturn(Optional.empty());

        expectThrows(IllegalStateException.class, () -> service.updateRoles(userId, Set.of(RoleType.ADMIN)));
    }

    @Test
    void updateUsernameIgnoresBlankAndRejectsDuplicates() {
        UUID userId = UUID.randomUUID();

        service.updateUsername(userId, " ");
        verify(authUsers, never()).existsByUsernameIgnoreCaseAndIdNot(any(), any());

        given(authUsers.existsByUsernameIgnoreCaseAndIdNot("admin", userId)).willReturn(true);
        expectThrows(ApiExceptions.ConflictException.class, () -> service.updateUsername(userId, " ADMIN "));
    }

    @Test
    void updateUsernamePersistsNormalizedUsernameWhenAvailable() {
        UUID userId = UUID.randomUUID();
        AuthUser user = authUser(userId, "user@example.com", "old", Set.of("USER"));
        given(authUsers.existsByUsernameIgnoreCaseAndIdNot("new.name", userId)).willReturn(false);
        given(authUsers.findById(userId)).willReturn(Optional.of(user));

        service.updateUsername(userId, " New.Name ");

        assertEquals(user.getUsername(), "new.name");
    }

    private AuthUser authUser(UUID userId, String email, String username, Set<String> roleNames) {
        AuthUser user = AuthUser.builder()
                .email(email)
                .username(username)
                .provider("LOCAL")
                .roles(roleNames.stream()
                        .map(role -> AuthRole.builder().id(UUID.randomUUID()).name(role).build())
                        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new)))
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
