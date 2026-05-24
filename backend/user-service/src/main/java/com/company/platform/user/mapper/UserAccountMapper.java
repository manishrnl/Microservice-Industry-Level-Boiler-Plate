package com.company.platform.user.mapper;

import com.company.platform.commons.dto.UserDto;
import com.company.platform.commons.enums.RoleType;
import com.company.platform.user.dto.AuthAccountDto;
import com.company.platform.user.dto.UserAccountSettingsDto;
import com.company.platform.user.dto.UserHeaderContextDto;
import com.company.platform.user.entity.AuthRole;
import com.company.platform.user.entity.AuthUser;
import com.company.platform.user.model.UserContactDetails;
import com.company.platform.user.model.UserIdentityDocument;
import com.company.platform.user.model.UserProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserAccountMapper {
    private final ModelMapper modelMapper;

    @PostConstruct
    void configureMappings() {
        if (modelMapper.getTypeMap(AuthUser.class, AuthAccountDto.class) == null) {
            modelMapper.createTypeMap(AuthUser.class, AuthAccountDto.class)
                    .addMappings(mapping -> {
                        mapping.skip(AuthAccountDto::setUserId);
                        mapping.skip(AuthAccountDto::setRoles);
                    });
        }
        if (modelMapper.getTypeMap(AuthAccountDto.class, UserDto.class) == null) {
            modelMapper.createTypeMap(AuthAccountDto.class, UserDto.class)
                    .addMappings(mapping -> mapping.skip(UserDto::setName));
        }
        if (modelMapper.getTypeMap(UserHeaderContextDto.class, UserDto.class) == null) {
            modelMapper.createTypeMap(UserHeaderContextDto.class, UserDto.class)
                    .addMappings(mapping -> {
                        mapping.skip(UserDto::setName);
                        mapping.skip(UserDto::setRoles);
                        mapping.skip(UserDto::setUsername);
                        mapping.skip(UserDto::setAvatarUrl);
                    });
        }
    }

    public AuthAccountDto toAuthAccountDto(AuthUser user) {
        AuthAccountDto dto = modelMapper.map(user, AuthAccountDto.class);
        dto.setUserId(user.getId());
        dto.setRoles(toRoleTypes(user.getRoles()));
        return dto;
    }

    public UserDto toUserDto(AuthAccountDto account, UserProfile profile) {
        UserDto dto = modelMapper.map(account, UserDto.class);
        dto.setName(displayName(profile == null ? null : profile.getName(), account.getUsername(), account.getEmail()));
        dto.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        dto.setRoles(defaultRoles(account.getRoles()));
        return dto;
    }

    public UserDto toUserDto(UserProfile profile, UserHeaderContextDto context, String username) {
        UserDto dto = modelMapper.map(context, UserDto.class);
        dto.setName(displayName(profile == null ? context.getName() : profile.getName(), username, context.getEmail()));
        dto.setUsername(username);
        dto.setAvatarUrl(profile == null ? null : profile.getAvatarUrl());
        dto.setRoles(parseRoles(context.getRoles()));
        return dto;
    }

    public UserAccountSettingsDto toAccountSettingsDto(UserHeaderContextDto context,
                                                       AuthAccountDto account,
                                                       UserProfile profile,
                                                       UserIdentityDocument identity,
                                                       UserContactDetails contact) {
        UserAccountSettingsDto dto = profile == null
                ? UserAccountSettingsDto.builder().build()
                : modelMapper.map(profile, UserAccountSettingsDto.class);
        if (identity != null) {
            modelMapper.map(identity, dto);
        }
        if (contact != null) {
            modelMapper.map(contact, dto);
        }
        dto.setUserId(context.getUserId());
        dto.setName(displayName(profile == null ? context.getName() : profile.getName(),
                account == null ? null : account.getUsername(),
                context.getEmail()));
        dto.setEmail(account == null || isBlank(account.getEmail()) ? context.getEmail() : account.getEmail());
        dto.setUsername(account == null ? null : account.getUsername());
        dto.setProvider(account == null ? null : account.getProvider());
        dto.setEmailVerified(account != null && account.isEmailVerified());
        dto.setAccountLocked(account != null && account.isAccountLocked());
        dto.setAccountStatus(firstNonBlank(account == null ? null : account.getAccountStatus(), "ACTIVE"));
        dto.setLockedUntil(account == null ? null : account.getLockedUntil());
        dto.setDeletedAt(account == null ? null : account.getDeletedAt());
        dto.setRoles(account == null ? parseRoles(context.getRoles()) : defaultRoles(account.getRoles()));
        dto.setCountry(firstNonBlank(dto.getCountry(), "India"));
        return dto;
    }

    public UserDto toDemoUserDto(UserProfile profile, String email, String username) {
        UserDto dto = modelMapper.map(profile, UserDto.class);
        dto.setEmail(email);
        dto.setUsername(username);
        dto.setRoles(Set.of(RoleType.USER));
        return dto;
    }

    public Set<RoleType> parseRoles(String roles) {
        if (isBlank(roles)) {
            return Set.of(RoleType.USER);
        }
        Set<RoleType> parsedRoles = java.util.Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        return defaultRoles(parsedRoles);
    }

    public String displayName(String profileName, String username, String email) {
        if (!isBlank(profileName) && !profileName.equalsIgnoreCase(email)) {
            return profileName;
        }
        if (!isBlank(username) && !username.equalsIgnoreCase(email)) {
            return username;
        }
        int atIndex = email == null ? -1 : email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : "User";
    }

    public Set<RoleType> defaultRoles(Set<RoleType> roles) {
        return roles == null || roles.isEmpty() ? Set.of(RoleType.USER) : roles;
    }

    private Set<RoleType> toRoleTypes(Set<AuthRole> roles) {
        Set<RoleType> parsed = (roles == null ? Set.<AuthRole>of() : roles).stream()
                .map(AuthRole::getName)
                .filter(Objects::nonNull)
                .map(RoleType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));
        return defaultRoles(parsed);
    }

    private String firstNonBlank(String primary, String fallback) {
        return isBlank(primary) ? fallback : primary;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
