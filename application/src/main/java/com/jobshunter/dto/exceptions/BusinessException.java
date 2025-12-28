package com.jobshunter.dto.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

  private final HttpStatus status;

  public BusinessException(HttpStatus status, String message) {
    this(status, message, null);
  }

  public BusinessException(HttpStatus status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

}
