package com.jobshunter.service.application.cost;

import com.jobshunter.database.service.JobOrderDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRequestCostEventListener {

  private final JobOrderDBService jobOrderDBService;

  private final RequestPriceService priceService;

  @Async
  @EventListener
  public void onAiRequestCost(AiRequestCostEvent event) {
    double cost = 0;
    Long orderId = event.getOrder().getJobOrder().getId();
    try {
      cost = priceService.calculatePrice(event.getTokensConsumed(), event.getOrder().getModel());
      if (cost > 0) {
        jobOrderDBService.addCostToOrder(orderId, cost);
      }
    } catch (Exception e) {
      log.warn("Failed to record cost {} for order {}: {}", cost, orderId, e.getMessage());
    }
  }
}
