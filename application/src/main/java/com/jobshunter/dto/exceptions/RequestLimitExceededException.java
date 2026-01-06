package com.jobshunter.dto.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when request rate limit or quota is exceeded. Used when rate limiting or quota enforcement is triggered. Returns HTTP 429 Too Many
 * Requests.
 */
@Getter
public class RequestLimitExceededException extends BusinessException {

  public RequestLimitExceededException(String message) {
    super(HttpStatus.TOO_MANY_REQUESTS, message);
  }

  public RequestLimitExceededException(String message, Throwable cause) {
    super(HttpStatus.TOO_MANY_REQUESTS, message, cause);
  }

}

