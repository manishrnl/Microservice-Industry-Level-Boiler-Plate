package com.company.platform.commons.api;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

class ApiContractsTest {

    @Test
    void okBuildsSuccessfulStandardResponseWithTraceId() {
        StandardApiResponse<Map<String, String>> response = StandardApiResponse.ok(
                "saved",
                Map.of("id", "42"),
                "trace-1"
        );

        assertTrue(response.success());
        assertEquals(response.status(), 200);
        assertEquals(response.message(), "saved");
        assertEquals(response.data().get("id"), "42");
        assertEquals(response.traceId(), "trace-1");
        assertNotNull(response.timestamp());
    }

    @Test
    void recordDtosKeepPagingAndProblemDetailsImmutable() {
        PagedResponse<String> page = new PagedResponse<>(List.of("a", "b"), 1, 2, 5, 3, false);
        ProblemDetails problem = new ProblemDetails(
                "https://httpstatuses.com/400",
                "Bad Request",
                400,
                "Validation failed",
                "/users",
                java.time.LocalDateTime.now(),
                "trace-2",
                Map.of("email", "must be valid")
        );

        assertEquals(page.content(), List.of("a", "b"));
        assertEquals(page.totalElements(), 5);
        assertFalse(page.last());
        assertEquals(problem.validationErrors().get("email"), "must be valid");
        assertEquals(problem.traceId(), "trace-2");
    }
}
