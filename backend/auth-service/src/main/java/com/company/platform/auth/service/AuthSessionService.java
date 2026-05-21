package com.company.platform.auth.service;

import com.company.platform.auth.dto.SessionInfoDto;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserSession;
import com.company.platform.auth.mapper.SessionInfoMapper;
import com.company.platform.auth.repository.UserSessionRepository;
import com.company.platform.commons.exception.ApiExceptions;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthSessionService {
    private static final int MAX_SESSIONS_PER_USER = 5;
    private static final int TOUCH_THROTTLE_SECONDS = 60;

    private final UserSessionRepository sessions;
    private final SessionInfoMapper mapper;

    public AuthSessionService(UserSessionRepository sessions, SessionInfoMapper mapper) {
        this.sessions = sessions;
        this.mapper = mapper;
    }

    public void create(User user, String sessionId, String deviceId, String ipAddress, String userAgent) {
        String normalizedDeviceId = blankToNull(deviceId);
        String normalizedIpAddress = blankToNull(ipAddress);
        String normalizedUserAgent = blankToNull(userAgent);
        deleteDuplicateDeviceSessions(user, normalizedDeviceId, normalizedIpAddress, normalizedUserAgent);
        sessions.save(UserSession.builder()
                .user(user)
                .sessionId(sessionId)
                .deviceId(normalizedDeviceId)
                .ipAddress(normalizedIpAddress)
                .userAgent(normalizedUserAgent)
                .lastActive(LocalDateTime.now())
                .expired(false)
                .build());
        deleteOldestOverLimit(user);
    }

    public UserSession requireActive(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ApiExceptions.UnauthorizedException("Session is expired or revoked");
        }
        return sessions.findBySessionIdAndExpiredFalse(sessionId)
                .orElseThrow(() -> new ApiExceptions.UnauthorizedException("Session is expired or revoked"));
    }

    public void touch(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.touchSessionBySessionId(sessionId);
        }
    }

    public void touchIfStale(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessions.findBySessionIdAndExpiredFalse(sessionId).ifPresent(this::touchIfStale);
    }

    public List<SessionInfoDto> list(User user, String currentSessionId) {
        UserSession currentSession = requireActive(currentSessionId);
        touchIfStale(currentSession);
        return sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user).stream()
                .map(session -> mapper.toDto(session, currentSessionId))
                .toList();
    }

    public void revoke(User user, String sessionId) {
        sessions.deleteByUserAndSessionId(user, sessionId);
    }

    public void revokeAll(User user) {
        sessions.deleteAllByUser(user);
    }

    public long countSessions() {
        return sessions.count();
    }

    private void deleteOldestOverLimit(User user) {
        List<UserSession> activeSessions = sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user);
        if (activeSessions.size() > MAX_SESSIONS_PER_USER) {
            sessions.deleteAll(activeSessions.subList(MAX_SESSIONS_PER_USER, activeSessions.size()));
        }
    }

    private void deleteDuplicateDeviceSessions(User user, String deviceId, String ipAddress, String userAgent) {
        if (deviceId != null) {
            sessions.deleteActiveByUserAndDeviceId(user, deviceId);
            if (userAgent != null) {
                sessions.deleteLegacyBrowserSessionsByUserAndUserAgent(user, userAgent);
            }
            return;
        }
        if (userAgent != null) {
            sessions.deleteLegacyBrowserSessionsByUserAndUserAgent(user, userAgent);
            return;
        }
        if (ipAddress != null) {
            sessions.deleteActiveByUserAndIpAddress(user, ipAddress);
        }
    }

    private void touchIfStale(UserSession session) {
        LocalDateTime lastActive = session.getLastActive();
        if (lastActive == null || lastActive.isBefore(LocalDateTime.now().minusSeconds(TOUCH_THROTTLE_SECONDS))) {
            sessions.touchSessionBySessionId(session.getSessionId());
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
