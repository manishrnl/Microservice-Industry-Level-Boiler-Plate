package com.company.platform.auth.security;

import com.company.platform.commons.enums.RoleType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenMinutes;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${security.jwt.issuer}") String issuer,
                           @Value("${security.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String createAccessToken(UUID userId, String email, Set<RoleType> roles, String sessionId) {
        return createAccessToken(userId, email, roles, sessionId, displayName(email));
    }

    public String createAccessToken(UUID userId, String email, Set<RoleType> roles, String sessionId, String name) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("name", name == null || name.isBlank() ? displayName(email) : name)
                .claim("roles", roles.stream().map(Enum::name).toList())
                .claim("sessionId", sessionId)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private String displayName(String email) {
        if (email == null || email.isBlank()) {
            return "User";
        }
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : email;
        return localPart.isBlank() ? "User" : localPart;
    }
}
