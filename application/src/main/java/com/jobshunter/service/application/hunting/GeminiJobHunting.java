package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserPromptEntity;
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
public final class GeminiJobHunting extends AiConversationJobHunting {

  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;

  private final ModelsDBService modelsDBService;

  public GeminiJobHunting(
      @Qualifier("geminiSearchExecutor") Executor geminiSearchExecutor,
      @Qualifier("JobsClientGemini") AiJobsClient geminiClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      ModelsDBService modelsDBService,
      AiConversationStateMachine conversationStateMachine,
      CountryIsoCode countryIsoCode
  ) {
    super(geminiSearchExecutor, geminiClient, userCvService, templateRenderer, conversationStateMachine, countryIsoCode);
    this.modelsDBService = modelsDBService;
  }

  @EventListener(ApplicationReadyEvent.class)
  private void init() {
    this.discoveryModel = modelsDBService.getModel(new EngineSelection(EngineType.GEMINI, "gemini-2.5-flash-lite")).orElseThrow();
    this.companiesModel = modelsDBService.getModel(new EngineSelection(EngineType.GEMINI, "gemini-2.5-flash-lite")).orElseThrow();
  }

  @Override
  public AIJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    AIJobSearchRequest request = super.createRequest(order, prompt);
    request.setDiscoveryModel(discoveryModel);
    request.setCompaniesModel(companiesModel);
    return request;
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.GEMINI;
  }

}
