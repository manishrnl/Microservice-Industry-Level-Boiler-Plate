package com.company.platform.auth.dto;

import com.company.platform.commons.dto.TokenDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponseDto {
    private TokenDto token;
    private String refreshCookie;
}
