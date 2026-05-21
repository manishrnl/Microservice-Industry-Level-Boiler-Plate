package com.company.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequestMetadataDto {
    private String ipAddress;
    private String userAgent;
    private String timeZone;
    private String localTime;
}
