package com.company.platform.audit.service;

import com.company.platform.audit.model.AuditRecord;
import com.company.platform.audit.repository.AuditRecordRepository;
import com.company.platform.commons.dto.DemoUserRequestDto;
import com.company.platform.commons.enums.AuditAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.testng.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

class AuditRecordServiceTest {
    @Mock
    private AuditRecordRepository repository;

    @BeforeMethod
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void queryAndExportMapStoredJsonToAuditDtos() {
        AuditRecord record = record("manish", "{\"before\":true}", "{\"after\":true}");
        AuditRecordService service = new AuditRecordService(repository, new ObjectMapper());
        given(repository.findTop100ByOrderByCreatedAtDesc()).willReturn(List.of(record));
        given(repository.findAll(any(Sort.class))).willReturn(List.of(record));

        var rows = service.query();
        assertEquals(rows.size(), 1);
        assertEquals(rows.getFirst().username(), "manish");
        assertEquals(rows.getFirst().beforeState().get("before"), true);
        assertEquals(rows.getFirst().afterState().get("after"), true);
        assertEquals(service.export().size(), 1);
    }

    @Test
    void invalidOrBlankJsonFallsBackToEmptyStateMaps() {
        AuditRecord record = record("manish", "not-json", "");
        AuditRecordService service = new AuditRecordService(repository, new ObjectMapper());
        given(repository.findTop100ByOrderByCreatedAtDesc()).willReturn(List.of(record));

        var rows = service.query();
        assertEquals(rows.size(), 1);
        assertTrue(rows.getFirst().beforeState().isEmpty());
        assertTrue(rows.getFirst().afterState().isEmpty());
    }

    @Test
    void seedDemoDataCreatesMissingTraceRowsAndUsesProvidedDisplayName() {
        UUID userId = UUID.randomUUID();
        DemoUserRequestDto request = new DemoUserRequestDto(userId, "u@example.com", "MANISH", "manish", null);
        AuditRecordService service = new AuditRecordService(repository, new ObjectMapper());
        given(repository.existsByUserIdAndTraceId(userId, "demo-user-created")).willReturn(false);
        given(repository.findTop100ByOrderByCreatedAtDesc()).willReturn(List.of());

        service.seedDemoData(request);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(repository, org.mockito.Mockito.times(5)).save(captor.capture());
        captor.getAllValues().forEach(record -> assertEquals(record.getUsername(), "MANISH"));
        List<String> traceIds = captor.getAllValues().stream().map(AuditRecord::getTraceId).toList();
        assertTrue(traceIds.contains("demo-user-created"));
        assertTrue(traceIds.contains("demo-login-success"));
        assertTrue(traceIds.contains("demo-role-assignment"));
    }

    @Test
    void seedDemoDataSkipsExistingTraceRowsAndFallsBackToEmailLocalPart() {
        UUID userId = UUID.randomUUID();
        DemoUserRequestDto request = new DemoUserRequestDto(userId, "person@example.com", "person@example.com", "person", null);
        AuditRecordService service = new AuditRecordService(repository, new ObjectMapper());
        given(repository.existsByUserIdAndTraceId(userId, "demo-user-created")).willReturn(true);
        given(repository.findTop100ByOrderByCreatedAtDesc()).willReturn(List.of());

        service.seedDemoData(request);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.argThat(record -> "demo-user-created".equals(record.getTraceId())));
        verify(repository, org.mockito.Mockito.times(4)).save(any(AuditRecord.class));
    }

    @Test
    void seedDemoDataUsesEmptyJsonWhenSerializationFails() throws Exception {
        UUID userId = UUID.randomUUID();
        ObjectMapper mapper = org.mockito.Mockito.mock(ObjectMapper.class);
        given(mapper.writeValueAsString(any())).willThrow(new IllegalStateException("no json"));
        given(repository.findTop100ByOrderByCreatedAtDesc()).willReturn(List.of());
        AuditRecordService service = new AuditRecordService(repository, mapper);

        service.seedDemoData(new DemoUserRequestDto(userId, null, null, null, null));

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(repository, org.mockito.Mockito.times(5)).save(captor.capture());
        assertEquals(captor.getAllValues().getFirst().getUsername(), "User");
        assertEquals(captor.getAllValues().getFirst().getBeforeState(), "{}");
        assertEquals(captor.getAllValues().getFirst().getAfterState(), "{}");
    }

    private AuditRecord record(String username, String beforeState, String afterState) {
        AuditRecord record = new AuditRecord();
        record.setUserId(UUID.randomUUID());
        record.setUsername(username);
        record.setAction(AuditAction.LOGIN);
        record.setResourceType("auth");
        record.setResourceId("session");
        record.setIpAddress("127.0.0.1");
        record.setUserAgent("Chrome");
        record.setTraceId("trace");
        record.setBeforeState(beforeState);
        record.setAfterState(afterState);
        record.setStatus("SUCCESS");
        return record;
    }
}
