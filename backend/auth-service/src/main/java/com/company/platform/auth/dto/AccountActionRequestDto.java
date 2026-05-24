package com.company.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountActionRequestDto {
    @NotBlank
    private String confirmation;

    private Integer days;
}
