package com.company.platform.auth.service;

import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.commons.enums.RoleType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthBootstrapDataTest {
    @Mock
    private UserRepository users;
    @Mock
    private RoleRepository roles;
    @Mock
    private UserRoleRepository userRoles;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private AuthDemoDataService demoData;

    @BeforeMethod
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void runCreatesSuperAdminWithAllBaselineRolesAndDemoData() {
        AuthBootstrapData bootstrap = new AuthBootstrapData(users, roles, userRoles, encoder, demoData);
        ReflectionTestUtils.setField(bootstrap, "superAdminEmail", " Admin@Example.com ");
        ReflectionTestUtils.setField(bootstrap, "superAdminUsername", " MANISH ");
        ReflectionTestUtils.setField(bootstrap, "superAdminPassword", "Password@123");
        given(roles.findByName(RoleType.USER.name())).willReturn(Optional.of(role(RoleType.USER)));
        given(roles.findByName(RoleType.ADMIN.name())).willReturn(Optional.of(role(RoleType.ADMIN)));
        given(roles.findByName(RoleType.SUPER_ADMIN.name())).willReturn(Optional.of(role(RoleType.SUPER_ADMIN)));
        given(users.existsByEmailIgnoreCase("admin@example.com")).willReturn(false);
        given(users.findByEmailIgnoreCase("admin@example.com")).willReturn(Optional.empty());
        given(encoder.encode("Password@123")).willReturn("hash");
        given(users.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        bootstrap.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertEquals(userCaptor.getValue().getEmail(), "admin@example.com");
        assertEquals(userCaptor.getValue().getUsername(), "manish");
        assertEquals(userCaptor.getValue().getPasswordHash(), "hash");
        assertTrue(userCaptor.getValue().isEmailVerified());
        verify(userRoles, org.mockito.Mockito.times(3)).save(any(UserRole.class));
        verify(demoData).provision(userCaptor.getValue());
    }

    private Role role(RoleType type) {
        return Role.builder().id(UUID.randomUUID()).name(type.name()).build();
    }
}
