package com.company.platform.commons.dto;

import com.company.platform.commons.enums.RoleType;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record RoleDto(UUID id, @NotNull RoleType name, Set<PermissionDto> permissions) {
}
