package com.company.platform.audit;

import com.company.platform.commons.dto.AuditEventDto;
import com.company.platform.commons.enums.AuditAction;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
class AuditController {
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<AuditEventDto> query() {
        return sampleEvents();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/export")
    List<AuditEventDto> export() {
        return sampleEvents();
    }

    private List<AuditEventDto> sampleEvents() {
        UUID userId = UUID.fromString("6dc557b4-32d2-4b5e-8b7c-357a88b9de14");
        return List.of(
                new AuditEventDto(
                        userId,
                        "Manish Sahu",
                        AuditAction.LOGIN,
                        "auth-session",
                        "current-session",
                        "172.20.0.1",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/148.0.0.0",
                        "sample-login-trace",
                        Map.of("session", Map.of("active", false)),
                        Map.of(
                                "actor", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "session", Map.of("id", "current-session", "active", true, "device", "Chrome on Windows", "ipAddress", "172.20.0.1"),
                                "description", "Manish Sahu signed in successfully from Chrome on Windows."
                        ),
                        "SUCCESS",
                        null,
                        LocalDateTime.now().minusMinutes(12)
                ),
                new AuditEventDto(
                        userId,
                        "Manish Sahu",
                        AuditAction.ROLE_CHANGE,
                        "user",
                        userId.toString(),
                        "127.0.0.1",
                        "Admin console",
                        "sample-role-trace",
                        Map.of(
                                "targetUser", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "roles", List.of("USER")
                        ),
                        Map.of(
                                "actor", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "targetUser", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "roles", List.of("USER", "ADMIN", "SUPER_ADMIN"),
                                "description", "Manish Sahu changed roles for Manish Sahu from USER to USER, ADMIN, SUPER_ADMIN."
                        ),
                        "SUCCESS",
                        null,
                        LocalDateTime.now().minusMinutes(8)
                ),
                new AuditEventDto(
                        userId,
                        "Manish Sahu",
                        AuditAction.SESSION_REVOKE,
                        "auth-session",
                        "old-device-session",
                        "127.0.0.1",
                        "Admin console",
                        "sample-session-trace",
                        Map.of(
                                "targetUser", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "session", Map.of("id", "old-device-session", "active", true, "device", "Android Chrome", "ipAddress", "172.20.0.8")
                        ),
                        Map.of(
                                "actor", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "targetUser", Map.of("id", userId.toString(), "name", "Manish Sahu", "email", "manishrajrnl@gmail.com"),
                                "session", Map.of("id", "old-device-session", "active", false, "device", "Android Chrome", "ipAddress", "172.20.0.8"),
                                "description", "Manish Sahu revoked the old Android Chrome session for Manish Sahu."
                        ),
                        "SUCCESS",
                        null,
                        LocalDateTime.now().minusMinutes(3)
                )
        );
    }
}
