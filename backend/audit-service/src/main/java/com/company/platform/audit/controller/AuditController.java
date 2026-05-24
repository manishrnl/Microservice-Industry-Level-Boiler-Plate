package com.company.platform.audit.controller;

import com.company.platform.audit.service.AuditRecordService;
import com.company.platform.commons.dto.AuditEventDto;
import com.company.platform.commons.dto.DemoUserRequestDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {
    private final AuditRecordService records;

    AuditController(AuditRecordService records) {
        this.records = records;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    List<AuditEventDto> query() {
        return records.query();
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/export")
    List<AuditEventDto> export() {
        return records.export();
    }

    @PostMapping("/internal/demo-data")
    @ResponseStatus(HttpStatus.CREATED)
    List<AuditEventDto> seedDemoData(@RequestBody DemoUserRequestDto request) {
        return records.seedDemoData(request);
    }
}
