package com.jobshunter.service.application.progress;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderProgressEventListener {

  private final OrderProgressStore orderProgressStore;

  @EventListener
  public void onOrderProgress(OrderProgressEvent event) {
    orderProgressStore.append(event.getOrderId(), event.getMessage());
  }
}
