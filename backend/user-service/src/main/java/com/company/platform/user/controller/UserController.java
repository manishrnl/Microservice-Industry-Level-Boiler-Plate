package com.company.platform.user.controller;

import com.company.platform.user.dto.AvatarUpdateRequestDto;
import com.company.platform.user.dto.AuthAccountDto;
import com.company.platform.user.dto.UserHeaderContextDto;
import com.company.platform.user.dto.UserAccountSettingsDto;
import com.company.platform.user.dto.UserAccountSettingsUpdateRequestDto;
import com.company.platform.user.dto.UserPreferenceUpdateRequestDto;
import com.company.platform.user.dto.UserProfileUpdateRequestDto;
import com.company.platform.user.dto.UserRoleUpdateRequestDto;
import com.company.platform.user.mapper.UserAccountMapper;
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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UserController {
    private final UserPreferenceRepository preferences;
    private final AuthUserAdminService authUsers;
    private final UserProfileRepository profiles;
    private final UserIdentityDocumentRepository identityDocuments;
    private final UserContactDetailsRepository contactDetails;
    private final UserAccountMapper userAccountMapper;

    @GetMapping("/me")
    UserDto me(@RequestHeader("X-User-Id") UUID userId,
               @RequestHeader("X-User-Email") String email,
               @RequestHeader(value = "X-User-Name", required = false) String name,
               @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        UserProfile profile = profiles.findById(userId).orElse(null);
        AuthAccountDto auth = authUsers.account(userId).orElse(null);
        return userAccountMapper.toUserDto(profile, headerContext(userId, email, name, roles), auth == null ? null : auth.getUsername());
    }

    @PutMapping("/me")
    UserDto updateMe(@RequestHeader("X-User-Id") UUID userId,
                     @RequestHeader("X-User-Email") String email,
                     @RequestHeader(value = "X-User-Name", required = false) String headerName,
                     @RequestHeader(value = "X-User-Roles", required = false) String roles,
                     @Valid @RequestBody UserProfileUpdateRequestDto request) {
        String name = userAccountMapper.displayName(request.getName(), null, email);
        UserProfile profile = profile(userId);
        profile.setName(name);
        profiles.save(profile);
        AuthAccountDto auth = authUsers.account(userId).orElse(null);
        return userAccountMapper.toUserDto(profile, headerContext(userId, email, headerName, roles), auth == null ? null : auth.getUsername());
    }

    @GetMapping("/me/settings")
    UserAccountSettingsDto accountSettings(@RequestHeader("X-User-Id") UUID userId,
                                           @RequestHeader("X-User-Email") String email,
                                           @RequestHeader(value = "X-User-Name", required = false) String headerName,
                                           @RequestHeader(value = "X-User-Roles", required = false) String headerRoles) {
        AuthAccountDto auth = authUsers.account(userId).orElse(null);
        UserProfile profile = profiles.findById(userId).orElse(null);
        UserIdentityDocument identity = identityDocuments.findById(userId).orElse(null);
        UserContactDetails contact = contactDetails.findById(userId).orElse(null);
        return userAccountMapper.toAccountSettingsDto(headerContext(userId, email, headerName, headerRoles), auth, profile, identity, contact);
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
    UserDto updateRole(@PathVariable UUID id, @Valid @RequestBody UserRoleUpdateRequestDto request) {
        return authUsers.updateRoles(id, request.toRoleTypes());
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
                                          @Valid @RequestBody UserPreferenceUpdateRequestDto request) {
        String timezone = request.getTimezone() == null ? "" : request.getTimezone();
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
                   @Valid @RequestBody AvatarUpdateRequestDto request) {
        UserProfile profile = profile(userId);
        profile.setAvatarUrl(blankToNull(request.getAvatarUrl()));
        profiles.save(profile);
        AuthAccountDto auth = authUsers.account(userId).orElse(null);
        return userAccountMapper.toUserDto(profile, headerContext(userId, email, name, roles), auth == null ? null : auth.getUsername());
    }

    @PostMapping("/internal/demo-data")
    UserDto seedDemoData(@RequestBody DemoUserRequestDto request) {
        UserProfile profile = profile(request.userId());
        if (profile.getName() == null || profile.getName().isBlank()) {
            profile.setName(userAccountMapper.displayName(request.name(), request.username(), request.email()));
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

        return userAccountMapper.toDemoUserDto(profile, request.email(), request.username());
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

    private String uppercaseBlankToNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String digitsOnly(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\D", "");
    }

    private UserHeaderContextDto headerContext(UUID userId, String email, String name, String roles) {
        return UserHeaderContextDto.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .roles(roles)
                .build();
    }
}
