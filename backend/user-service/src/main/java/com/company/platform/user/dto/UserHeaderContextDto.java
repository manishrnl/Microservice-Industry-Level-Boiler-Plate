package com.company.platform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHeaderContextDto {
    private UUID userId;
    private String email;
    private String name;
    private String roles;
}
