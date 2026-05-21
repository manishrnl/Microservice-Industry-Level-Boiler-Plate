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

    private final UserSessionRepository sessions;
    private final SessionInfoMapper mapper;

    public AuthSessionService(UserSessionRepository sessions, SessionInfoMapper mapper) {
        this.sessions = sessions;
        this.mapper = mapper;
    }

    public void create(User user, String sessionId, String deviceId, String ipAddress, String userAgent) {
        if (ipAddress != null && !ipAddress.isBlank()) {
            sessions.deleteByUserAndIpAddress(user, ipAddress);
        }
        sessions.save(UserSession.builder()
                .user(user)
                .sessionId(sessionId)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
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

    public List<SessionInfoDto> list(User user, String currentSessionId) {
        requireActive(currentSessionId);
        touch(currentSessionId);
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

    private void deleteOldestOverLimit(User user) {
        List<UserSession> activeSessions = sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user);
        if (activeSessions.size() > MAX_SESSIONS_PER_USER) {
            sessions.deleteAll(activeSessions.subList(MAX_SESSIONS_PER_USER, activeSessions.size()));
        }
    }
}
