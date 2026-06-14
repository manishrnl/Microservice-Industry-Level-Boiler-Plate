package com.company.platform.auth.controller;

import com.company.platform.auth.email.BrevoMailDiagnosticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/mail")
public class MailDiagnosticsController {
    private final BrevoMailDiagnosticsService brevoMailDiagnosticsService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/brevo/diagnostics")
    public Map<String, Object> brevoDiagnostics() {
        return brevoMailDiagnosticsService.diagnostics();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/brevo/test")
    public Map<String, Object> sendBrevoTest(@RequestParam String to) {
        return brevoMailDiagnosticsService.sendTest(to);
    }
}
