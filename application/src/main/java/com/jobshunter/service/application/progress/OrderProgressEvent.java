package com.jobshunter.service.application.progress;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Internal event carrying a progress message linked to a job order.
 */
@Getter
public class OrderProgressEvent extends ApplicationEvent {

  private final Long orderId;
  private final String message;

  public OrderProgressEvent(Object source, Long orderId, String message) {
    super(source);
    this.orderId = orderId;
    this.message = message;
  }
}
