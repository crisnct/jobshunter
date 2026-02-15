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
    Long orderId = event.getJobOrderId();
    try {
      cost = priceService.calculatePrice(event.getTokensConsumed(), event.getModel());
      log.info("Calculate request price for model {}, {} input  tokens, {} output tokens ... => cost={} ",
          event.getModel().getModel(),
          event.getTokensConsumed().inputTokens(),
          event.getTokensConsumed().outputTokens(),
          cost
      );
      if (cost > 0) {
        int diff =
            event.getTokensConsumed().inputTokens() + event.getTokensConsumed().outputTokens() - event.getEstmTokens().estimatedTotalTokens();
        log.info("Difference between estimated and real cost: {} tokens", diff);
        jobOrderDBService.addCostToOrder(orderId, cost);
      }
    } catch (Exception e) {
      log.warn("Failed to record cost {} for order {}: {}", cost, orderId, e.getMessage());
    }
  }
}
