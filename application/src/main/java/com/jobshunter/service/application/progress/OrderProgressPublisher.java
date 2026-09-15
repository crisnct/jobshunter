package com.jobshunter.service.application.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Centralized way to publish progress checkpoints for a given order id.
 */
@Service
@RequiredArgsConstructor
public class OrderProgressPublisher {

  private final ApplicationEventPublisher eventPublisher;

  public void emit(Long orderId, String message) {
    if (orderId == null || !StringUtils.hasText(message)) {
      return;
    }
    eventPublisher.publishEvent(new OrderProgressEvent(this, orderId, message));
  }
}
