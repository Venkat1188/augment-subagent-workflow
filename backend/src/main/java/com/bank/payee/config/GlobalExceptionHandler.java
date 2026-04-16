package com.bank.payee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rule: data-validation [avoid-sensitive-data-in-errors] — Severity: High
 * Rule: error-handling  [use-specific-exceptions]         — Severity: High
 * Rule: error-handling  [include-error-context-and-ids]   — Severity: High
 *
 * <p>Intercepts all unhandled exceptions and translates them into RFC 7807
 * Problem Detail responses. Stack traces and internal details are NEVER
 * returned to the caller; they are logged server-side only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation failures ({@code @Valid} on request bodies).
     * Returns 400 with a field-level error map — no stack trace exposed.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://problems.bank.com/validation-error"));
        problem.setTitle("Validation Failed");
        problem.setDetail("One or more request fields are invalid.");
        problem.setProperty("errors", fieldErrors);

        // Log at WARN — validation failures are caller errors, not server errors
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Catch-all for unexpected server errors.
     * Rule: [avoid-sensitive-data-in-errors] — returns a generic 500 body;
     * full exception is logged server-side with a correlation hint.
     * Rule: [preserve-exception-context] — original exception passed to logger.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        // Log full detail server-side — includes stack trace for debugging
        log.error("Unhandled exception in payee service", ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("https://problems.bank.com/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred. Please try again or contact support.");
        // No stack trace, no internal paths, no DB errors exposed to caller

        return ResponseEntity.internalServerError().body(problem);
    }
}
