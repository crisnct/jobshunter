package com.jobshunter.dto.exceptions;

import org.springframework.security.core.AuthenticationException;

public class DeviceRevokedException extends AuthenticationException {

  public DeviceRevokedException(String message) {
    super(message);
  }
}
