package com.company.platform.auth.mapper;

import com.company.platform.auth.entity.User;
import com.company.platform.auth.entity.UserSession;
import com.company.platform.commons.config.ModelMapperConfig;
import com.company.platform.commons.enums.RoleType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;

class AuthMappersTest {

    @Test
    void authUserMapperUsesUsernameOrEmailFallback() {
        AuthUserMapper mapper = new AuthUserMapper(new ModelMapperConfig().modelMapper());
        UUID userId = UUID.randomUUID();
        User user = User.builder().email("person@example.com").username("").provider("LOCAL").build();
        ReflectionTestUtils.setField(user, "id", userId);

        var dto = mapper.toDto(user, Set.of(RoleType.USER));
        var direct = mapper.toDto(userId, "MANISH", "manish@example.com", Set.of(RoleType.ADMIN), "/a.png");

        assertEquals(dto.getName(), "person");
        assertTrue(dto.getUsername().isEmpty());
        assertEquals(direct.getName(), "MANISH");
        assertEquals(direct.getAvatarUrl(), "/a.png");
    }

    @Test
    void sessionInfoMapperParsesDeviceInfoAndCurrentFlag() {
        SessionInfoMapper mapper = new SessionInfoMapper(new ModelMapperConfig().modelMapper());
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .sessionId("session-1")
                .deviceId("browser-123")
                .ipAddress(null)
                .userAgent("Mozilla/5.0 (Windows NT 10.0) Chrome/120.0 Safari/537.36")
                .createdAt(LocalDateTime.parse("2026-05-24T10:00:00"))
                .lastActive(null)
                .expired(false)
                .build();

        var dto = mapper.toDto(session, "session-1");

        assertEquals(dto.getDeviceId(), "Browser session");
        assertTrue(dto.getIpAddress().isEmpty());
        assertEquals(dto.getBrowser(), "Chrome");
        assertEquals(dto.getOperatingSystem(), "Windows");
        assertEquals(dto.getDeviceType(), "Desktop");
        assertTrue(dto.isCurrent());
        assertEquals(dto.getLastActive(), "1970-01-01T00:00:00Z");
    }

    @DataProvider
    Object[][] deviceUserAgents() {
        return new Object[][]{
                {"Mozilla/5.0 (Mac OS X) Edg/120.0 Safari/537.36", "Microsoft Edge", "macOS", "Desktop"},
                {"Mozilla/5.0 (X11; Linux x86_64) Firefox/120.0", "Firefox", "Linux", "Desktop"},
                {"Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/604.1", "Safari", "iOS", "Mobile"},
                {"Mozilla/5.0 (Linux; Android 14; Pixel 8) Chrome/120.0 Mobile Safari/537.36", "Chrome", "Android", "Mobile"},
                {"Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) Safari/604.1", "Safari", "iOS", "Tablet"}
        };
    }

    @Test(dataProvider = "deviceUserAgents")
    void sessionInfoMapperClassifiesCommonUserAgents(String userAgent, String browser, String os, String deviceType) {
        SessionInfoMapper mapper = new SessionInfoMapper(new ModelMapperConfig().modelMapper());
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .sessionId("session-2")
                .deviceId("trusted-laptop")
                .ipAddress("203.0.113.10")
                .userAgent(userAgent)
                .createdAt(null)
                .lastActive(LocalDateTime.parse("2026-05-24T11:00:00"))
                .expired(true)
                .build();

        var dto = mapper.toDto(session, "other-session");

        assertEquals(dto.getDeviceId(), "trusted-laptop");
        assertEquals(dto.getIpAddress(), "203.0.113.10");
        assertEquals(dto.getBrowser(), browser);
        assertEquals(dto.getOperatingSystem(), os);
        assertEquals(dto.getDeviceType(), deviceType);
        assertFalse(dto.isCurrent());
        assertEquals(dto.getCreatedAt(), "1970-01-01T00:00:00Z");
        assertEquals(dto.getLastActive(), "2026-05-24T11:00:00Z");
    }

    @Test
    void sessionInfoMapperUsesUnknownLabelsForBlankUserAgentAndDeviceId() {
        SessionInfoMapper mapper = new SessionInfoMapper(new ModelMapperConfig().modelMapper());
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .sessionId("session-3")
                .deviceId(" ")
                .userAgent(" ")
                .build();

        var dto = mapper.toDto(session, "other-session");

        assertEquals(dto.getDeviceId(), "Browser session");
        assertEquals(dto.getBrowser(), "Unknown browser");
        assertEquals(dto.getOperatingSystem(), "Unknown OS");
        assertEquals(dto.getDeviceType(), "Unknown device");
    }
}
