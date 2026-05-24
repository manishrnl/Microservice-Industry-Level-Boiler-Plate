package com.company.platform.commons.dto;

import java.util.UUID;

public record DemoUserRequestDto(UUID userId, String email, String name, String username, String avatarUrl) {
}
