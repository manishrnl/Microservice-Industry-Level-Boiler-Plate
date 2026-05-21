package com.company.platform.audit;

import com.company.platform.commons.dto.AuditEventDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
class AuditController {
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<AuditEventDto> query() {
        return List.of();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/export")
    String export() {
        return "[]";
    }
}
