package com.jobshunter.service.application.cost;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokensConsumed;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Published when an AI request completes and its cost has been calculated. Used to persist cost to the job_order table without coupling low-level
 * clients to DB services.
 */
@Getter
public class AiRequestCostEvent extends ApplicationEvent {

  private final TokensConsumed tokensConsumed;

  private final Long jobOrderId;

  private final AiModelEntity model;

  public AiRequestCostEvent(Object source, Long jobOrderId, AiModelEntity model, TokensConsumed tokensConsumed) {
    super(source);
    this.jobOrderId = jobOrderId;
    this.model = model;
    this.tokensConsumed = tokensConsumed;
  }

}
