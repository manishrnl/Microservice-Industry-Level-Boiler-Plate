package com.company.platform.commons.exception;

import com.company.platform.commons.api.ProblemDetails;
import jakarta.validation.ConstraintViolationException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.Set;

import static org.testng.Assert.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterMethod
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void mapsDomainExceptionsToExpectedHttpStatuses() {
        assertProblem(handler.notFound(new ApiExceptions.ResourceNotFoundException("missing")), HttpStatus.NOT_FOUND);
        assertProblem(handler.badRequest(new ApiExceptions.ValidationException("bad")), HttpStatus.BAD_REQUEST);
        assertProblem(handler.badRequest(new ConstraintViolationException("bad", Set.of())), HttpStatus.BAD_REQUEST);
        assertProblem(handler.unauthorized(new ApiExceptions.UnauthorizedException("no")), HttpStatus.UNAUTHORIZED);
        assertProblem(handler.forbidden(new ApiExceptions.ForbiddenException("stop")), HttpStatus.FORBIDDEN);
        assertProblem(handler.conflict(new ApiExceptions.ConflictException("same")), HttpStatus.CONFLICT);
        assertProblem(handler.rateLimited(new ApiExceptions.RateLimitExceededException("slow")), HttpStatus.TOO_MANY_REQUESTS);
        assertProblem(handler.unavailable(new ApiExceptions.ServiceUnavailableException("down")), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void mapsFrameworkExceptionsWithStableMessagesAndTraceId() {
        MDC.put("traceId", "trace-123");

        ResponseEntity<ProblemDetails> malformed = handler.malformedJson(new HttpMessageNotReadableException("bad json"));
        ResponseEntity<ProblemDetails> method = handler.methodNotAllowed(new HttpRequestMethodNotSupportedException("PATCH"));
        ResponseEntity<ProblemDetails> dataIntegrity = handler.dataIntegrity(new DataIntegrityViolationException("duplicate"));
        ResponseEntity<ProblemDetails> dataAccess = handler.dataAccess(new DataAccessResourceFailureException("down"));
        ResponseEntity<ProblemDetails> generic = handler.generic(new RuntimeException("boom"));

        assertEquals(malformed.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertEquals(malformed.getBody().detail(), "Malformed JSON request body");
        assertEquals(method.getStatusCode(), HttpStatus.METHOD_NOT_ALLOWED);
        assertEquals(dataIntegrity.getStatusCode(), HttpStatus.CONFLICT);
        assertEquals(dataAccess.getStatusCode(), HttpStatus.SERVICE_UNAVAILABLE);
        assertEquals(generic.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        assertEquals(generic.getBody().traceId(), "trace-123");
    }

    @Test
    void validationProblemIncludesFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationFixture(), "request");
        bindingResult.rejectValue("email", "Email", "must be valid");

        ResponseEntity<ProblemDetails> response = ReflectionTestUtils.invokeMethod(handler, "validationProblem", bindingResult);

        assertEquals(response.getStatusCode(), HttpStatus.BAD_REQUEST);
        assertEquals(response.getBody().detail(), "Validation failed");
        assertEquals(response.getBody().validationErrors().get("email"), "must be valid");
    }

    private void assertProblem(ResponseEntity<ProblemDetails> response, HttpStatus status) {
        assertEquals(response.getStatusCode(), status);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().status(), status.value());
    }

    private static final class ValidationFixture {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
