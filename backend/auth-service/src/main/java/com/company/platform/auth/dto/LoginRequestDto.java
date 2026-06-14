package com.company.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    @Size(max = 254)
    private String identifier;

    @Size(max = 254)
    private String email;

    @NotBlank
    @Size(max = 128)
    private String password;

    @Size(max = 255)
    private String deviceId;
}
