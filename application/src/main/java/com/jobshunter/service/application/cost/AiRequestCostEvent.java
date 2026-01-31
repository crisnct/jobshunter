package com.jobshunter.service.application.cost;

import com.jobshunter.dto.TokensConsumed;
import com.jobshunter.model.SearchJobOrder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when an AI request completes and its cost has been calculated. Used to persist cost to the job_order table without coupling low-level
 * clients to DB services.
 */
@Getter
public class AiRequestCostEvent extends ApplicationEvent {

  private final SearchJobOrder order;
  private final TokensConsumed tokensConsumed;

  public AiRequestCostEvent(Object source, SearchJobOrder order, TokensConsumed tokensConsumed) {
    super(source);
    this.order = order;
    this.tokensConsumed = tokensConsumed;
  }

}
