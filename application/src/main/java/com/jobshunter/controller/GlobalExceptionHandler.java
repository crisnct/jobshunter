package com.jobshunter.controller;

import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.dto.exceptions.RequestLimitExceededException;
import com.jobshunter.dto.exceptions.ResourceNotFoundException;
import com.jobshunter.dto.exceptions.ValidationException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler for all REST controller advice. Centralized exception handling with consistent error response format.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String ERROR_MESSAGE = "errorMessage";

  /**
   * Handles validation exceptions - HTTP 400 Bad Request.
   */
  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<Map<String, String>> handleValidationException(ValidationException exception) {
    log.warn("Validation error: {}", exception.getMessage());
    return ResponseEntity.status(exception.getStatus())
        .body(Map.of(ERROR_MESSAGE, exception.getMessage()));
  }

  /**
   * Handles resource not found exceptions - HTTP 404 Not Found.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleResourceNotFoundException(
      ResourceNotFoundException exception) {
    log.warn("Resource not found: {}", exception.getMessage());
    return ResponseEntity.status(exception.getStatus())
        .body(Map.of(ERROR_MESSAGE, exception.getMessage()));
  }

  /**
   * Handles request limit exceeded exceptions - HTTP 429 Too Many Requests.
   */
  @ExceptionHandler(RequestLimitExceededException.class)
  public ResponseEntity<Map<String, String>> handleRequestLimitExceededException(
      RequestLimitExceededException exception) {
    log.warn("Request limit exceeded: {}", exception.getMessage());
    return ResponseEntity.status(exception.getStatus())
        .body(Map.of(ERROR_MESSAGE, exception.getMessage()));
  }

  /**
   * Handles generic business exceptions.
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<Map<String, String>> handleBusinessException(BusinessException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      message = ExceptionUtils.getMessage(exception);
    }
    log.error(message, exception);
    return ResponseEntity.status(exception.getStatus()).body(Map.of(ERROR_MESSAGE, message));
  }

  /**
   * Handles legacy ResponseStatusException - for backward compatibility.
   */
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatusException(
      ResponseStatusException exception) {
    log.warn("ResponseStatusException (deprecated): {}", exception.getReason());
    String message = exception.getReason();
    if (message == null || message.isBlank()) {
      message = exception.getMessage();
    }
    //noinspection ConstantValue
    if (message == null || message.isBlank()) {
      message = ExceptionUtils.getMessage(exception);
    }
    return ResponseEntity.status(exception.getStatusCode()).body(Map.of(ERROR_MESSAGE, message));
  }

  @SuppressWarnings("DataFlowIssue")
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleBindException(MethodArgumentNotValidException exception) {
    MethodParameter parameter = exception.getParameter();
    String message = parameter.getDeclaringClass().getName()
        + " -> " + parameter.getMethod().getName()
        + " -> " + exception.getFieldError().getDefaultMessage();
    return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleBindException(Exception exception) {
    log.error("Unexpected exception", exception);
    return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
  }

}
