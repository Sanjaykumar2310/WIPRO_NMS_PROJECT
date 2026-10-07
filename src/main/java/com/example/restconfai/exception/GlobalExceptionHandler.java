package com.example.restconfai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 *
 * What: Centralized error handling for all REST controllers.
 * Why: Provides consistent, user-friendly error responses without exposing internals.
 * How: Intercepts exceptions and converts them to structured JSON responses.
 * Example: RestconfException → {error, message, statusCode, timestamp}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RestconfException.class)
    public ResponseEntity<Map<String, Object>> handleRestconfException(RestconfException ex) {
        logger.error("RESTCONF error: {} [device={}, operation={}]",
                ex.getMessage(), ex.getDeviceName(), ex.getOperation());

        Map<String, Object> error = buildErrorResponse(
                "RESTCONF Error", ex.getMessage(), ex.getStatusCode());
        error.put("device", ex.getDeviceName());
        error.put("operation", ex.getOperation());
        error.put("troubleshooting", getTroubleshootingTips(ex.getStatusCode()));

        return ResponseEntity.status(mapStatusCode(ex.getStatusCode())).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(
                err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));

        Map<String, Object> error = buildErrorResponse(
                "Validation Error", "Input validation failed", 400);
        error.put("fieldErrors", fieldErrors);

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        Map<String, Object> error = buildErrorResponse(
                "Bad Request", ex.getMessage(), 400);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        logger.warn("Malformed JSON request body: {}", ex.getMessage());
        Map<String, Object> error = buildErrorResponse(
                "Bad Request", "Malformed JSON request body or missing payload", 400);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        // Never expose internal details to client
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        Map<String, Object> error = buildErrorResponse(
                "Internal Server Error",
                "An unexpected error occurred. Please try again.",
                500);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private Map<String, Object> buildErrorResponse(String error, String message, int statusCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("statusCode", statusCode);
        body.put("timestamp", LocalDateTime.now().toString());
        return body;
    }

    private @NonNull HttpStatus mapStatusCode(int code) {
        try {
            return HttpStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private String getTroubleshootingTips(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Verify the request payload and parameters.";
            case 401 -> "Check RESTCONF credentials (username/password).";
            case 403 -> "The user may not have permission for this operation.";
            case 404 -> "The requested resource does not exist on the device.";
            case 408 -> "Connection timed out. Check network connectivity and device status.";
            case 409 -> "Resource conflict. The resource may already exist or is locked.";
            case 502, 503 -> "The RESTCONF server may be unavailable. Verify the device is running.";
            default -> "Check device connectivity, RESTCONF service, and credentials.";
        };
    }
}
