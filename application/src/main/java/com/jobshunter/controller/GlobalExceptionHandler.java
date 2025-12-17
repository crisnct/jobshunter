package com.jobshunter.controller;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException exception) {
    String message = exception.getReason();
    if (message == null || message.isBlank()) {
      message = exception.getMessage();
    }
    //noinspection ConstantValue
    if (message == null || message.isBlank()) {
      message = ExceptionUtils.getMessage(exception);
    }
    return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", message));
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

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, String>> handleBindException(RuntimeException exception) {
    log.error("Unexpected exception", exception);
    return ResponseEntity.internalServerError().body(Map.of("message", exception.getMessage()));
  }


}
