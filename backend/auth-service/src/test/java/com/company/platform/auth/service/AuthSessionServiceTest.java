package com.company.platform.auth.service;

import com.company.platform.auth.dto.SessionInfoDto;
import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserSession;
import com.company.platform.auth.mapper.SessionInfoMapper;
import com.company.platform.auth.repository.UserSessionRepository;
import com.company.platform.commons.exception.ApiExceptions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthSessionServiceTest {
    @Mock
    private UserSessionRepository sessions;
    @Mock
    private SessionInfoMapper mapper;

    @BeforeMethod
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createNormalizesDeviceDataDeletesDuplicatesAndPrunesOldSessions() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        List<UserSession> active = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> UserSession.builder().sessionId("s" + index).lastActive(LocalDateTime.now().minusMinutes(index)).build())
                .toList();
        given(sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user)).willReturn(active);

        service.create(user, "session-new", " device-1 ", " 127.0.0.1 ", " Chrome ");

        verify(sessions).deleteActiveByUserAndDeviceId(user, "device-1");
        verify(sessions).deleteLegacyBrowserSessionsByUserAndUserAgent(user, "Chrome");
        ArgumentCaptor<UserSession> saved = ArgumentCaptor.forClass(UserSession.class);
        verify(sessions).save(saved.capture());
        assertEquals(saved.getValue().getDeviceId(), "device-1");
        assertEquals(saved.getValue().getIpAddress(), "127.0.0.1");
        verify(sessions).deleteAll(active.subList(5, 6));
    }

    @Test
    void createDeletesLegacyBrowserOrIpDuplicatesWhenDeviceIsMissing() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        given(sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user)).willReturn(List.of());

        service.create(user, "session-ua", "", "", "Chrome");
        service.create(user, "session-ip", "", "127.0.0.1", "");

        verify(sessions).deleteLegacyBrowserSessionsByUserAndUserAgent(user, "Chrome");
        verify(sessions).deleteActiveByUserAndIpAddress(user, "127.0.0.1");
    }

    @Test
    void suspiciousLoginIsFalseForNoSessionsAndFalseForKnownDevice() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        given(sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user))
                .willReturn(List.of(), List.of(UserSession.builder().deviceId("device-1").ipAddress("127.0.0.1").userAgent("Chrome").build()));

        assertFalse(service.isSuspiciousLogin(user, "device-1", "127.0.0.1", "Chrome"));
        assertFalse(service.isSuspiciousLogin(user, "device-1", "127.0.0.1", "Chrome"));
    }

    @Test
    void suspiciousLoginIsTrueWhenNoKnownSessionMatches() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        given(sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user))
                .willReturn(List.of(UserSession.builder().deviceId("old").ipAddress("127.0.0.1").userAgent("Chrome").build()));

        assertTrue(service.isSuspiciousLogin(user, "new", "203.0.113.1", "Firefox"));
    }

    @Test
    void requireActiveRejectsBlankOrMissingSessions() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        given(sessions.findBySessionIdAndExpiredFalse("missing")).willReturn(Optional.empty());

        expectThrows(ApiExceptions.UnauthorizedException.class, () -> service.requireActive(" "));
        expectThrows(ApiExceptions.UnauthorizedException.class, () -> service.requireActive("missing"));
    }

    @Test
    void touchAndTouchIfStaleThrottleRepositoryWrites() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        UserSession old = UserSession.builder().sessionId("old").lastActive(LocalDateTime.now().minusMinutes(2)).build();
        UserSession fresh = UserSession.builder().sessionId("fresh").lastActive(LocalDateTime.now()).build();
        given(sessions.findBySessionIdAndExpiredFalse("old")).willReturn(Optional.of(old));
        given(sessions.findBySessionIdAndExpiredFalse("fresh")).willReturn(Optional.of(fresh));

        service.touch("");
        service.touch("direct");
        service.touchIfStale(null);
        service.touchIfStale("old");
        service.touchIfStale("fresh");

        verify(sessions, never()).touchSessionBySessionId("");
        verify(sessions).touchSessionBySessionId("direct");
        verify(sessions).touchSessionBySessionId("old");
        verify(sessions, never()).touchSessionBySessionId("fresh");
    }

    @Test
    void listRequiresCurrentSessionTouchesWhenStaleAndMapsDtos() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        UserSession current = UserSession.builder().sessionId("current").lastActive(LocalDateTime.now().minusMinutes(2)).build();
        SessionInfoDto dto = SessionInfoDto.builder().sessionId("current").current(true).build();
        given(sessions.findBySessionIdAndExpiredFalse("current")).willReturn(Optional.of(current));
        given(sessions.findByUserAndExpiredFalseOrderByLastActiveDescCreatedAtDesc(user)).willReturn(List.of(current));
        given(mapper.toDto(current, "current")).willReturn(dto);

        assertEquals(service.list(user, "current"), List.of(dto));

        verify(sessions).touchSessionBySessionId("current");
    }

    @Test
    void revokeAndCountDelegateToRepository() {
        AuthSessionService service = new AuthSessionService(sessions, mapper);
        User user = User.builder().email("u@example.com").build();
        given(sessions.count()).willReturn(7L);

        service.revoke(user, "session-1");
        service.revokeAll(user);

        assertEquals(service.countSessions(), 7L);
        verify(sessions).deleteByUserAndSessionId(user, "session-1");
        verify(sessions).deleteAllByUser(user);
    }
}
