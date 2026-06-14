package com.company.platform.user.dto;

import com.company.platform.commons.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthAccountDto {
    private UUID userId;
    private String email;
    private String username;
    private String provider;
    private boolean emailVerified;
    private boolean accountLocked;
    private int failedAttempts;
    private String accountStatus;
    private LocalDateTime lockedUntil;
    private LocalDateTime deletedAt;
    private Set<RoleType> roles;
}
