package com.company.platform.commons.exception;

import com.company.platform.commons.api.ProblemDetails;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiExceptions.ResourceNotFoundException.class)
    ResponseEntity<ProblemDetails> notFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({ApiExceptions.ValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetails> badRequest(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.UnauthorizedException.class)
    ResponseEntity<ProblemDetails> unauthorized(RuntimeException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.ForbiddenException.class)
    ResponseEntity<ProblemDetails> forbidden(RuntimeException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.ConflictException.class)
    ResponseEntity<ProblemDetails> conflict(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.RateLimitExceededException.class)
    ResponseEntity<ProblemDetails> rateLimited(RuntimeException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.ServiceUnavailableException.class)
    ResponseEntity<ProblemDetails> unavailable(RuntimeException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetails> invalid(MethodArgumentNotValidException ex) {
        return validationProblem(ex.getBindingResult());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ProblemDetails> invalidReactive(WebExchangeBindException ex) {
        return validationProblem(ex.getBindingResult());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetails> malformedJson(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed JSON request body", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetails> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), Map.of());
    }

    /**
     * Covers unique constraint violations, FK failures, etc. More specific than {@link DataAccessException}.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetails> dataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT,
                "This email may already be registered, or the submitted data conflicts with existing records.",
                Map.of());
    }

    /**
     * JDBC / connection / missing table–style failures (often misconfigured {@code AUTH_DATABASE_URL} or Flyway not run).
     */
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ProblemDetails> dataAccess(DataAccessException ex) {
        log.error("Database access error", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE,
                "Database is unavailable or misconfigured. Ensure PostgreSQL is reachable and Flyway migrations have been applied to auth_db.",
                Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetails> generic(Exception ex) {
        log.error("Unhandled API exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", Map.of());
    }

    private ResponseEntity<ProblemDetails> validationProblem(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    private ResponseEntity<ProblemDetails> problem(HttpStatus status, String detail, Map<String, String> errors) {
        ProblemDetails body = new ProblemDetails(
                "https://httpstatuses.com/" + status.value(),
                status.getReasonPhrase(),
                status.value(),
                detail,
                null,
                LocalDateTime.now(),
                MDC.get("traceId"),
                errors
        );
        return ResponseEntity.status(status).body(body);
    }
}
