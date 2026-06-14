package com.company.platform.auth.config;

import org.springframework.security.oauth2.jwt.Jwt;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

class SecurityConfigTest {
    @Test
    void jwtAuthenticationConverterMapsRolesClaimToSpringRoleAuthorities() {
        SecurityConfig config = new SecurityConfig();
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", "user-1", "roles", List.of("SUPER_ADMIN")));

        var authentication = config.jwtAuthenticationConverter().convert(jwt);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority())));
    }
}
