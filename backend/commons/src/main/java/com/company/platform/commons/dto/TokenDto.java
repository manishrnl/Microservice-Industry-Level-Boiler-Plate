package com.company.platform.commons.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    @NotBlank
    private String accessToken;

    private String tokenType;
    private long expiresInSeconds;
}
