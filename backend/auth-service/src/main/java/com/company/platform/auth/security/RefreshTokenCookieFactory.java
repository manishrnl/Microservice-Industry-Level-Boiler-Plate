package com.company.platform.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {
    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshTokenCookieFactory(@Value("${security.cookies.secure:false}") boolean secure,
                                     @Value("${security.jwt.refresh-token-days:7}") long refreshTokenDays) {
        this.secure = secure;
        this.maxAgeSeconds = refreshTokenDays * 24 * 60 * 60;
    }

    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from("refresh_token", rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .build();
    }
}
