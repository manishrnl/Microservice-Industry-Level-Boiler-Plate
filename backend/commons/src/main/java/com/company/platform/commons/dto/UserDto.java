package com.company.platform.commons.dto;

import com.company.platform.commons.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID userId;

    @NotBlank
    private String name;

    @Email
    private String email;

    private String username;
    private Set<RoleType> roles;
    private String avatarUrl;
}
