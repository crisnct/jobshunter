package com.jobshunter.controller;

import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatusException(
      ResponseStatusException exception) {
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
}
