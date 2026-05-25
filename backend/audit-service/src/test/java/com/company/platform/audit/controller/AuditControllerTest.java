package com.company.platform.audit.controller;

import com.company.platform.audit.service.AuditRecordService;
import com.company.platform.commons.dto.AuditEventDto;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.enums.AuditAction;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.BDDMockito.given;

class AuditControllerTest {
    private final AuditRecordService service = mock(AuditRecordService.class);
    private final AuditController controller = new AuditController(service);

    @Test
    void delegatesQueryExportAndDemoSeed() {
        AuditEventDto event = event();
        DemoUserRequestDto demo = new DemoUserRequestDto(UUID.randomUUID(), "u@example.com", "User", "user", null);
        given(service.query()).willReturn(List.of(event));
        given(service.export()).willReturn(List.of(event, event));
        given(service.seedDemoData(demo)).willReturn(List.of(event));

        assertEquals(controller.query(), List.of(event));
        assertEquals(controller.export().size(), 2);
        assertEquals(controller.seedDemoData(demo), List.of(event));
    }

    private AuditEventDto event() {
        return new AuditEventDto(UUID.randomUUID(), "user", AuditAction.LOGIN, "auth", "session",
                "127.0.0.1", "Chrome", "trace", Map.of(), Map.of(), "SUCCESS", null, LocalDateTime.now());
    }
}
