package com.company.platform.user;

import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UserController {
    @GetMapping("/me")
    UserDto me(@RequestHeader("X-User-Id") UUID userId,
               @RequestHeader("X-User-Email") String email,
               @RequestHeader(value = "X-User-Name", required = false) String name,
               @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return new UserDto(userId, displayName(name, email), email, parseRoles(roles), null);
    }

    @PutMapping("/me")
    UserDto updateMe(@RequestHeader("X-User-Id") UUID userId,
                     @RequestHeader("X-User-Email") String email,
                     @RequestHeader(value = "X-User-Roles", required = false) String roles,
                     @RequestBody Map<String, Object> request) {
        String name = request.get("name") instanceof String value && !value.isBlank() ? value : displayName(null, email);
        return new UserDto(userId, name, email, parseRoles(roles), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    UserDto get(@PathVariable UUID id) {
        return new UserDto(id, "Managed User", "managed-user@localhost", Set.of(RoleType.USER), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<UserDto> search() {
        return List.of();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    void updateRole(@PathVariable UUID id, @RequestBody Map<String, String> request) {
    }

    @GetMapping("/me/preferences")
    Map<String, Object> preferences() {
        return Map.of("timezone", "UTC");
    }

    @PutMapping("/me/preferences")
    Map<String, Object> updatePreferences(@RequestBody Map<String, Object> preferences) {
        return preferences;
    }

    @PostMapping("/me/avatar")
    Map<String, String> avatar() {
        return Map.of("status", "uploaded");
    }

    private String displayName(String name, String email) {
        if (name != null && !name.isBlank() && !name.equalsIgnoreCase(email)) {
            return name;
        }
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    private Set<RoleType> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Set.of(RoleType.USER);
        }
        return java.util.Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(RoleType::valueOf)
                .collect(java.util.stream.Collectors.toSet());
    }
}
