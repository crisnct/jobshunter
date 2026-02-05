package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.dto.AIJobSearchRequest;
import com.jobshunter.model.EngineSelection;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class GptJobHunting extends AiConversationJobHunting {

  private final ModelsDBService modelsDBService;
  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;

  public GptJobHunting(
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor,
      @Qualifier("JobsClientGPT") AiJobsClient gptClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      ModelsDBService modelsDBService,
      AiConversationStateMachine conversationStateMachine,
      CountryIsoCode countryIsoCode
  ) {
    super(gptSearchExecutor, gptClient, userCvService, templateRenderer, conversationStateMachine, countryIsoCode);
    this.modelsDBService = modelsDBService;
  }

  @EventListener(ApplicationReadyEvent.class)
  private void init() {
    this.companiesModel = modelsDBService.getModel(new EngineSelection(EngineType.GPT, "gpt-4o-mini-2024-07-18")).orElseThrow();
    this.discoveryModel = modelsDBService.getModel(new EngineSelection(EngineType.GPT, "gpt-4o-mini-2024-07-18")).orElseThrow();
  }

  @Override
  public AIJobSearchRequest createRequest(SearchJobOrder order) {
    return super.createRequest(order).toBuilder()
        .discoveryModel(discoveryModel)
        .companiesModel(companiesModel)
        .build();
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.GPT;
  }


}
