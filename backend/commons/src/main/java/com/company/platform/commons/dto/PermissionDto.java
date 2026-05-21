package com.company.platform.commons.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PermissionDto(UUID id, @NotBlank String name, String resource, String action) {
}
