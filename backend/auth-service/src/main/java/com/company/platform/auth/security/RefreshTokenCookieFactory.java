package com.company.platform.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {
    private final boolean secure;
    private final String sameSite;
    private final long maxAgeSeconds;

    public RefreshTokenCookieFactory(@Value("${security.cookies.secure:false}") boolean secure,
                                     @Value("${security.cookies.same-site:Lax}") String sameSite,
                                     @Value("${security.jwt.refresh-token-days:7}") long refreshTokenDays) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeSeconds = refreshTokenDays * 24 * 60 * 60;
    }

    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1/auth/refresh")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .build();
    }
}
