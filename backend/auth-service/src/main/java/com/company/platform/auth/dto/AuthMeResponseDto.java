package com.company.platform.auth.dto;

import com.company.platform.commons.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthMeResponseDto {
    private UserDto user;
    private String accessToken;
}
