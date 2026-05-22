package com.company.platform.user;

import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
class UserController {
    private final UserPreferenceRepository preferences;
    private final AuthUserAdminService authUsers;

    UserController(UserPreferenceRepository preferences, AuthUserAdminService authUsers) {
        this.preferences = preferences;
        this.authUsers = authUsers;
    }

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
        return authUsers.get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<UserDto> search(@RequestParam(value = "q", required = false) String query,
                         @RequestParam(value = "role", required = false) String role) {
        return authUsers.search(query, role);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    void delete(@PathVariable UUID id) {
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    UserDto updateRole(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        Set<RoleType> roles = requestedRoles(request);
        return authUsers.updateRoles(id, roles);
    }

    @GetMapping("/me/preferences")
    Map<String, Object> preferences(@RequestHeader("X-User-Id") UUID userId,
                                    @RequestHeader(value = "X-Client-Time-Zone", required = false) String clientTimezone) {
        String timezone = preferences.findById(userId)
                .map(UserPreference::getTimezone)
                .orElseGet(() -> validTimezoneOrDefault(clientTimezone));
        return Map.of("timezone", timezone);
    }

    @PutMapping("/me/preferences")
    Map<String, Object> updatePreferences(@RequestHeader("X-User-Id") UUID userId,
                                          @RequestBody Map<String, Object> request) {
        String timezone = request.get("timezone") instanceof String value ? value : "";
        timezone = validTimezoneOrDefault(timezone);
        UserPreference preference = preferences.findById(userId).orElseGet(UserPreference::new);
        preference.setUserId(userId);
        preference.setTimezone(timezone);
        preferences.save(preference);
        return Map.of("timezone", timezone);
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

    private Set<RoleType> requestedRoles(Map<String, Object> request) {
        Object roles = request.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(String::toUpperCase)
                    .map(RoleType::valueOf)
                    .collect(java.util.stream.Collectors.toSet());
        }
        Object role = request.get("role");
        if (role instanceof String value && !value.isBlank()) {
            return Set.of(RoleType.valueOf(value.trim().toUpperCase()));
        }
        return Set.of(RoleType.USER);
    }

    private String validTimezoneOrDefault(String timezone) {
        try {
            if (timezone != null && !timezone.isBlank()) {
                return ZoneId.of(timezone).getId();
            }
        } catch (DateTimeException ignored) {
        }
        return "UTC";
    }
}
