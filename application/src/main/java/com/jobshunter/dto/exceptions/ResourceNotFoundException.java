package com.jobshunter.dto.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found. Used when querying for entities by ID or other identifiers that don't exist. Returns HTTP
 * 404 Not Found.
 */
@Getter
public class ResourceNotFoundException extends BusinessException {

  public ResourceNotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(HttpStatus.NOT_FOUND, message, cause);
  }

}

