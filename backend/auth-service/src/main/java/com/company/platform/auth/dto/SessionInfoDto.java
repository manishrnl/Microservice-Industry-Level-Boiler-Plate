package com.company.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDto {
    private UUID id;
    private String sessionId;
    private String deviceId;
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String operatingSystem;
    private String deviceType;
    private String createdAt;
    private String lastActive;
    private boolean current;
    private boolean expired;
}
