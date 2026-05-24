package com.company.platform.user.dto;

import com.company.platform.commons.enums.RoleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserAccountSettingsDto {
    private UUID userId;
    private String name;
    private String email;
    private String username;
    private String avatarUrl;
    private String provider;
    private boolean emailVerified;
    private boolean accountLocked;
    private String accountStatus;
    private LocalDateTime lockedUntil;
    private LocalDateTime deletedAt;
    private Set<RoleType> roles;
    private String aadhaarNumber;
    private String panNumber;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String addressLine;
    private String city;
    private String state;
    private String country;
    private String postalCode;
}
