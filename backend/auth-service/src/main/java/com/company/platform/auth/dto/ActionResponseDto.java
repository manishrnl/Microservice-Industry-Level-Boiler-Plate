package com.company.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponseDto {
    private String status;
    private String email;
    private String channel;
    private String delivery;
    private Boolean revoked;
    private Boolean revokedCurrent;
}
