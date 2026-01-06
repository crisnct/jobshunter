package com.jobshunter.dto.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when validation of input data fails. Used for input validation errors from controllers or services. Returns HTTP 400 Bad Request.
 */
@Getter
public class ValidationException extends BusinessException {

  public ValidationException(String message) {
    super(HttpStatus.BAD_REQUEST, message);
  }

  public ValidationException(String message, Throwable cause) {
    super(HttpStatus.BAD_REQUEST, message, cause);
  }

}

