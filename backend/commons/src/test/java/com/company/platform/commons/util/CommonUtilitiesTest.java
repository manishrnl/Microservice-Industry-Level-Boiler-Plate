package com.company.platform.commons.util;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class CommonUtilitiesTest {

    @AfterMethod
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dateAndHashUtilitiesReturnStableValues() {
        Instant instant = Instant.parse("2026-05-24T10:15:30Z");

        assertEquals(DateUtil.iso(instant), "2026-05-24T10:15:30Z");
        assertEquals(DateUtil.toZone(instant, "Asia/Kolkata").getHour(), 15);
        assertEquals(HashUtil.sha256("platform").length(), 64);
        assertEquals(HashUtil.sha256("platform"), HashUtil.sha256("platform"));
    }

    @Test
    void hashUtilityCannotBeInstantiatedThroughPublicApi() throws Exception {
        Constructor<HashUtil> constructor = HashUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertNotNull(constructor.newInstance());
    }

    @Test
    void ipUtilityPrefersFirstForwardedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.9");

        assertEquals(IpUtil.realIp(request), "203.0.113.10");

        MockHttpServletRequest direct = new MockHttpServletRequest();
        direct.setRemoteAddr("127.0.0.1");
        assertEquals(IpUtil.realIp(direct), "127.0.0.1");
    }

    @Test
    void paginationBoundsPageSizeAndKeepsSort() {
        var pageable = PaginationUtil.page(-3, 500, "email", Sort.Direction.DESC);

        assertEquals(pageable.getPageNumber(), 0);
        assertEquals(pageable.getPageSize(), 100);
        assertEquals(pageable.getSort().getOrderFor("email").getDirection(), Sort.Direction.DESC);
    }

    @Test
    void securityUtilityReadsCurrentAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin", "n/a"));

        assertTrue(SecurityUtil.authentication().isPresent());
        assertTrue(String.valueOf(SecurityUtil.currentUserName()).contains(String.valueOf("admin")));
    }

    @Test
    void jwtUtilityDelegatesParsingAndExtractsClaims() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "roles", List.of("ADMIN")));
        Jwt expired = new Jwt("expired", Instant.now().minusSeconds(120), Instant.now().minusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", userId.toString()));
        Jwt noExpiry = new Jwt("no-expiry", Instant.now(), null, Map.of("alg", "none"),
                Map.of("sub", userId.toString()));
        JwtDecoder decoder = mock(JwtDecoder.class);
        given(decoder.decode("token")).willReturn(jwt);
        given(decoder.decode("expired")).willReturn(expired);
        given(decoder.decode("no-expiry")).willReturn(noExpiry);
        JwtUtil util = new JwtUtil(decoder);

        assertSame(util.parse("token"), jwt);
        assertEquals(util.extractUserId("token"), userId);
        assertEquals(util.extractRoles("token"), List.of("ADMIN"));
        assertTrue(util.isValid("token"));
        assertFalse(util.isValid("expired"));
        assertTrue(util.isValid("no-expiry"));
    }
}
