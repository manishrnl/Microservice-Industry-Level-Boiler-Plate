package com.company.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminPasswordUpdateRequestDto {
    @NotBlank
    @Size(min = 8, max = 120)
    private String password;

    @NotBlank
    @Size(min = 8, max = 120)
    private String confirmPassword;
}
