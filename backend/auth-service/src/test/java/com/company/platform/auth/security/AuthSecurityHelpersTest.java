package com.company.platform.auth.security;

import com.company.platform.commons.enums.RoleType;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuthSecurityHelpersTest {

    @Test
    void refreshTokenCookieFactoryCreatesAndClearsHttpOnlyCookie() {
        RefreshTokenCookieFactory factory = new RefreshTokenCookieFactory(true, "Strict", 2);

        ResponseCookie created = factory.create("refresh-1");
        ResponseCookie cleared = factory.clear();

        assertTrue(String.valueOf(created.toString()).contains(String.valueOf("refresh_token=refresh-1")));
        assertTrue(String.valueOf(created.toString()).contains(String.valueOf("HttpOnly")));
        assertTrue(String.valueOf(created.toString()).contains(String.valueOf("Secure")));
        assertTrue(String.valueOf(created.toString()).contains(String.valueOf("SameSite=Strict")));
        assertTrue(String.valueOf(created.toString()).contains(String.valueOf("Max-Age=172800")));
        assertTrue(String.valueOf(cleared.toString()).contains(String.valueOf("refresh_token=")));
        assertTrue(String.valueOf(cleared.toString()).contains(String.valueOf("Max-Age=0")));
    }

    @Test
    void rsaKeyServiceExposesPrivateKeyAndPublicJwks() {
        RsaKeyService service = new RsaKeyService("test-key");

        assertTrue(service.rsaKey().isPrivate());
        assertTrue(service.rsaKey().getKeyID().startsWith("test-key-"));
        assertTrue(service.jwks().containsKey("keys"));
    }

    @Test
    void jwtTokenServiceBuildsExpectedClaimsAndFallbackDisplayName() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        given(encoder.encode(org.mockito.ArgumentMatchers.any())).willReturn(
                new Jwt("encoded-token", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "RS256"), Map.of("sub", "subject"))
        );
        JwtTokenService service = new JwtTokenService(encoder, "platform", 15);
        UUID userId = UUID.randomUUID();

        String token = service.createAccessToken(userId, "manish@example.com", Set.of(RoleType.ADMIN), "session-1", "", null);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(captor.capture());
        JwtClaimsSet claims = captor.getValue().getClaims();
        assertEquals(token, "encoded-token");
        assertEquals(claims.getClaim("iss").toString(), "platform");
        assertEquals(claims.getSubject(), userId.toString());
        assertEquals(claims.getClaim("name").toString(), "manish");
        assertTrue(claims.getClaim("username").toString().isEmpty());
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getClaim("roles");
        assertTrue(String.valueOf(roles).contains(String.valueOf("ADMIN")));
        assertEquals(claims.getClaim("sessionId").toString(), "session-1");
    }
}
