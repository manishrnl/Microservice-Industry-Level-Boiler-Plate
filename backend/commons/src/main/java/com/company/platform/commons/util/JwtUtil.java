package com.company.platform.commons.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class JwtUtil {
    private final JwtDecoder decoder;

    public JwtUtil(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    public Jwt parse(String token) {
        return decoder.decode(token);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public List<String> extractRoles(String token) {
        return parse(token).getClaimAsStringList("roles");
    }

    public boolean isValid(String token) {
        Jwt jwt = parse(token);
        return jwt.getExpiresAt() == null || jwt.getExpiresAt().isAfter(Instant.now());
    }
}
