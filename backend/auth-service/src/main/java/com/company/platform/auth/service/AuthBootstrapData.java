package com.company.platform.auth.service;

import com.company.platform.auth.entity.Role;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserRole;
import com.company.platform.auth.entity.UserRoleId;
import com.company.platform.auth.repository.RoleRepository;
import com.company.platform.auth.repository.UserRepository;
import com.company.platform.auth.repository.UserRoleRepository;
import com.company.platform.commons.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuthBootstrapData implements ApplicationRunner {
    private static final String ACCOUNT_STATUS_ACTIVE = "ACTIVE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthDemoDataService demoDataService;

    @Value("${app.bootstrap-super-admin-email:manishrajrnl@zohomail.in}")
    private String superAdminEmail;

    @Value("${app.bootstrap-super-admin-username:manish}")
    private String superAdminUsername;

    @Value("${app.bootstrap-super-admin-password:Password@123}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role userRole = role(RoleType.USER);
        Role adminRole = role(RoleType.ADMIN);
        Role superAdminRole = role(RoleType.SUPER_ADMIN);

        String email = normalizeEmail(superAdminEmail);
        boolean existingUser = userRepository.existsByEmailIgnoreCase(email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> User.builder()
                        .email(email)
                        .provider("LOCAL")
                        .build());
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.setUsername(normalizeUsername(superAdminUsername));
        }
        if (!existingUser || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
        }
        user.setEmailVerified(true);
        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        user.setAccountStatus(ACCOUNT_STATUS_ACTIVE);
        User saved = userRepository.save(user);

        assign(saved, userRole);
        assign(saved, adminRole);
        assign(saved, superAdminRole);
        demoDataService.provision(saved);
    }

    private Role role(RoleType roleType) {
        return roleRepository.findByName(roleType.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleType.name()).build()));
    }

    private void assign(User user, Role role) {
        if (userRoleRepository.existsByUserAndRole(user, role)) {
            return;
        }
        userRoleRepository.save(UserRole.builder()
                .id(UserRoleId.builder()
                        .userId(user.getId())
                        .roleId(role.getId())
                        .build())
                .user(user)
                .role(role)
                .build());
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? "manishrajrnl@zohomail.in" : email.trim().toLowerCase();
    }

    private String normalizeUsername(String username) {
        return username == null || username.isBlank() ? "manish" : username.trim().toLowerCase();
    }
}
