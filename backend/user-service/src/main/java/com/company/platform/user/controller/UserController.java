package com.company.platform.user.controller;

import com.company.platform.user.dto.AvatarUpdateRequestDto;
import com.company.platform.user.dto.UserAccountSettingsDto;
import com.company.platform.user.dto.UserAccountSettingsUpdateRequestDto;
import com.company.platform.user.model.UserContactDetails;
import com.company.platform.user.model.UserIdentityDocument;
import com.company.platform.user.model.UserPreference;
import com.company.platform.user.model.UserProfile;
import com.company.platform.user.repository.UserContactDetailsRepository;
import com.company.platform.user.repository.UserIdentityDocumentRepository;
import com.company.platform.user.repository.UserPreferenceRepository;
import com.company.platform.user.repository.UserProfileRepository;
import com.company.platform.user.service.AuthUserAdminService;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import jakarta.validation.Valid;
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
public class UserController {
    private final UserPreferenceRepository preferences;
    private final AuthUserAdminService authUsers;
    private final UserProfileRepository profiles;
    private final UserIdentityDocumentRepository identityDocuments;
    private final UserContactDetailsRepository contactDetails;

    public UserController(UserPreferenceRepository preferences,
                          AuthUserAdminService authUsers,
                          UserProfileRepository profiles,
                          UserIdentityDocumentRepository identityDocuments,
                          UserContactDetailsRepository contactDetails) {
        this.preferences = preferences;
        this.authUsers = authUsers;
        this.profiles = profiles;
        this.identityDocuments = identityDocuments;
        this.contactDetails = contactDetails;
    }

    @GetMapping("/me")
    UserDto me(@RequestHeader("X-User-Id") UUID userId,
               @RequestHeader("X-User-Email") String email,
               @RequestHeader(value = "X-User-Name", required = false) String name,
               @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        UserProfile profile = profiles.findById(userId).orElse(null);
        String username = authUsers.account(userId).map(AuthUserAdminService.AuthAccount::username).orElse(null);
        return new UserDto(userId, displayName(profile == null ? name : profile.getName(), email), email, username, parseRoles(roles), profile == null ? null : profile.getAvatarUrl());
    }

    @PutMapping("/me")
    UserDto updateMe(@RequestHeader("X-User-Id") UUID userId,
                     @RequestHeader("X-User-Email") String email,
                     @RequestHeader(value = "X-User-Roles", required = false) String roles,
                     @RequestBody Map<String, Object> request) {
        String name = request.get("name") instanceof String value && !value.isBlank() ? value : displayName(null, email);
        UserProfile profile = profile(userId);
        profile.setName(name);
        profiles.save(profile);
        String username = authUsers.account(userId).map(AuthUserAdminService.AuthAccount::username).orElse(null);
        return new UserDto(userId, name, email, username, parseRoles(roles), profile.getAvatarUrl());
    }

    @GetMapping("/me/settings")
    UserAccountSettingsDto accountSettings(@RequestHeader("X-User-Id") UUID userId,
                                           @RequestHeader("X-User-Email") String email,
                                           @RequestHeader(value = "X-User-Name", required = false) String headerName,
                                           @RequestHeader(value = "X-User-Roles", required = false) String headerRoles) {
        AuthUserAdminService.AuthAccount auth = authUsers.account(userId).orElse(null);
        UserProfile profile = profiles.findById(userId).orElse(null);
        UserIdentityDocument identity = identityDocuments.findById(userId).orElse(null);
        UserContactDetails contact = contactDetails.findById(userId).orElse(null);
        String username = auth == null ? null : auth.username();
        return UserAccountSettingsDto.builder()
                .userId(userId)
                .name(displayName(profile == null ? headerName : profile.getName(), email))
                .email(auth == null ? email : auth.email())
                .username(username)
                .avatarUrl(profile == null ? null : profile.getAvatarUrl())
                .provider(auth == null ? null : auth.provider())
                .emailVerified(auth != null && auth.emailVerified())
                .accountLocked(auth != null && auth.accountLocked())
                .accountStatus(auth == null || auth.accountStatus() == null ? "ACTIVE" : auth.accountStatus())
                .lockedUntil(auth == null ? null : auth.lockedUntil())
                .deletedAt(auth == null ? null : auth.deletedAt())
                .roles(auth == null ? parseRoles(headerRoles) : auth.roles())
                .aadhaarNumber(firstNonBlank(identity == null ? null : identity.getAadhaarNumber(), profile == null ? null : profile.getAadhaarNumber()))
                .panNumber(firstNonBlank(identity == null ? null : identity.getPanNumber(), profile == null ? null : profile.getPanNumber()))
                .phoneNumber(firstNonBlank(contact == null ? null : contact.getPhoneNumber(), profile == null ? null : profile.getPhoneNumber()))
                .dateOfBirth(profile == null ? null : profile.getDateOfBirth())
                .addressLine(firstNonBlank(contact == null ? null : contact.getAddressLine(), profile == null ? null : profile.getAddressLine()))
                .city(firstNonBlank(contact == null ? null : contact.getCity(), profile == null ? null : profile.getCity()))
                .state(firstNonBlank(contact == null ? null : contact.getState(), profile == null ? null : profile.getState()))
                .country(firstNonBlank(contact == null ? null : contact.getCountry(), profile == null ? null : profile.getCountry()))
                .postalCode(firstNonBlank(contact == null ? null : contact.getPostalCode(), profile == null ? null : profile.getPostalCode()))
                .build();
    }

    @PutMapping("/me/settings")
    UserAccountSettingsDto updateAccountSettings(@RequestHeader("X-User-Id") UUID userId,
                                                 @RequestHeader("X-User-Email") String email,
                                                 @RequestHeader(value = "X-User-Name", required = false) String headerName,
                                                 @RequestHeader(value = "X-User-Roles", required = false) String roles,
                                                 @Valid @RequestBody UserAccountSettingsUpdateRequestDto request) {
        UserProfile profile = profile(userId);
        profile.setName(blankToNull(request.getName()));
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setAadhaarNumber(digitsOnly(request.getAadhaarNumber()));
        profile.setPanNumber(uppercaseBlankToNull(request.getPanNumber()));
        profile.setPhoneNumber(blankToNull(request.getPhoneNumber()));
        profile.setAddressLine(blankToNull(request.getAddressLine()));
        profile.setCity(blankToNull(request.getCity()));
        profile.setState(blankToNull(request.getState()));
        profile.setCountry(blankToNull(request.getCountry()));
        profile.setPostalCode(blankToNull(request.getPostalCode()));
        profiles.save(profile);
        authUsers.updateUsername(userId, request.getUsername());

        UserIdentityDocument identity = identity(userId);
        identity.setAadhaarNumber(digitsOnly(request.getAadhaarNumber()));
        identity.setPanNumber(uppercaseBlankToNull(request.getPanNumber()));
        identityDocuments.save(identity);

        UserContactDetails contact = contact(userId);
        contact.setPhoneNumber(blankToNull(request.getPhoneNumber()));
        contact.setAddressLine(blankToNull(request.getAddressLine()));
        contact.setCity(blankToNull(request.getCity()));
        contact.setState(blankToNull(request.getState()));
        contact.setCountry(blankToNull(request.getCountry()));
        contact.setPostalCode(blankToNull(request.getPostalCode()));
        contactDetails.save(contact);

        return accountSettings(userId, email, headerName, roles);
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

    @PutMapping("/me/avatar")
    UserDto avatar(@RequestHeader("X-User-Id") UUID userId,
                   @RequestHeader("X-User-Email") String email,
                   @RequestHeader(value = "X-User-Name", required = false) String name,
                   @RequestHeader(value = "X-User-Roles", required = false) String roles,
                   @RequestBody AvatarUpdateRequestDto request) {
        UserProfile profile = profile(userId);
        profile.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        profiles.save(profile);
        String username = authUsers.account(userId).map(AuthUserAdminService.AuthAccount::username).orElse(null);
        return new UserDto(userId, displayName(profile.getName() == null ? name : profile.getName(), email), email, username, parseRoles(roles), profile.getAvatarUrl());
    }

    @PostMapping("/internal/demo-data")
    UserDto seedDemoData(@RequestBody DemoUserRequestDto request) {
        UserProfile profile = profile(request.userId());
        if (profile.getName() == null || profile.getName().isBlank()) {
            profile.setName(displayName(request.name(), request.email()));
        }
        if (profile.getAvatarUrl() == null || profile.getAvatarUrl().isBlank()) {
            profile.setAvatarUrl(blankToNull(request.avatarUrl()));
        }
        profiles.save(profile);

        UserPreference preference = preferences.findById(request.userId()).orElseGet(UserPreference::new);
        preference.setUserId(request.userId());
        if (preference.getTimezone() == null || preference.getTimezone().isBlank()) {
            preference.setTimezone("Asia/Kolkata");
        }
        preferences.save(preference);

        return new UserDto(request.userId(), profile.getName(), request.email(), request.username(), Set.of(RoleType.USER), profile.getAvatarUrl());
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

    private UserProfile profile(UUID userId) {
        UserProfile profile = profiles.findById(userId).orElseGet(UserProfile::new);
        profile.setUserId(userId);
        return profile;
    }

    private UserIdentityDocument identity(UUID userId) {
        UserIdentityDocument identity = identityDocuments.findById(userId).orElseGet(UserIdentityDocument::new);
        identity.setUserId(userId);
        return identity;
    }

    private UserContactDetails contact(UUID userId) {
        UserContactDetails contact = contactDetails.findById(userId).orElseGet(UserContactDetails::new);
        contact.setUserId(userId);
        return contact;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private String uppercaseBlankToNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String digitsOnly(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\D", "");
    }
}
