package com.company.platform.auth.mapper;

import com.company.platform.auth.dto.SessionInfoDto;
import com.company.platform.auth.entity.UserSession;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class SessionInfoMapper {
    private final ModelMapper modelMapper;

    public SessionInfoMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public SessionInfoDto toDto(UserSession session, String currentSessionId) {
        SessionInfoDto dto = modelMapper.map(new SessionView(session.getId(), session.getSessionId(), session.isExpired()), SessionInfoDto.class);
        DeviceInfo deviceInfo = parseDeviceInfo(session.getUserAgent());
        dto.setDeviceId(displayDeviceId(session.getDeviceId()));
        dto.setIpAddress(session.getIpAddress() == null ? "" : session.getIpAddress());
        dto.setUserAgent(session.getUserAgent() == null ? "" : session.getUserAgent());
        dto.setBrowser(deviceInfo.browser());
        dto.setOperatingSystem(deviceInfo.operatingSystem());
        dto.setDeviceType(deviceInfo.deviceType());
        dto.setCreatedAt(toUtcInstant(session.getCreatedAt()));
        dto.setLastActive(session.getLastActive() == null ? Instant.EPOCH.toString() : toUtcInstant(session.getLastActive()));
        dto.setCurrent(session.getSessionId().equals(currentSessionId));
        return dto;
    }

    private String toUtcInstant(LocalDateTime value) {
        return value == null ? Instant.EPOCH.toString() : value.atOffset(ZoneOffset.UTC).toInstant().toString();
    }

    private DeviceInfo parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new DeviceInfo("Unknown browser", "Unknown OS", "Unknown device");
        }
        String browser = userAgent.contains("Edg/") ? "Microsoft Edge"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Safari/") ? "Safari"
                : "Browser";
        String os = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS"
                : userAgent.contains("Mac OS X") ? "macOS"
                : userAgent.contains("Linux") ? "Linux"
                : "Unknown OS";
        String deviceType = userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone") ? "Mobile"
                : userAgent.contains("iPad") || userAgent.contains("Tablet") ? "Tablet"
                : "Desktop";
        return new DeviceInfo(browser, os, deviceType);
    }

    private String displayDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank() || deviceId.startsWith("browser-")) {
            return "Browser session";
        }
        return deviceId;
    }

    private record DeviceInfo(String browser, String operatingSystem, String deviceType) {
    }

    private static final class SessionView {
        private final java.util.UUID id;
        private final String sessionId;
        private final boolean expired;

        private SessionView(java.util.UUID id, String sessionId, boolean expired) {
            this.id = id;
            this.sessionId = sessionId;
            this.expired = expired;
        }

        public java.util.UUID getId() {
            return id;
        }

        public String getSessionId() {
            return sessionId;
        }

        public boolean isExpired() {
            return expired;
        }
    }
}
