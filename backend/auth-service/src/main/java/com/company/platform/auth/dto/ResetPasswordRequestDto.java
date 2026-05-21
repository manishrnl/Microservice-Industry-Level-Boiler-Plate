package com.company.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDto {
    @NotBlank
    private String email;

    @NotBlank
    private String otp;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;
}
