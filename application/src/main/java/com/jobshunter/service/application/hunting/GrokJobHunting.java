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
public final class GrokJobHunting extends AiConversationJobHunting {

  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;
  private final ModelsDBService modelsDBService;

  public GrokJobHunting(
      @Qualifier("grokSearchExecutor") Executor executor,
      @Qualifier("JobsClientGROK") AiJobsClient aiClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      ModelsDBService modelsDBService,
      AiConversationStateMachine conversationStateMachine,
      CountryIsoCode countryIsoCode
  ) {
    super(executor, aiClient, userCvService, templateRenderer, conversationStateMachine, countryIsoCode);
    this.modelsDBService = modelsDBService;
  }

  @EventListener(ApplicationReadyEvent.class)
  private void init() {
    this.discoveryModel = modelsDBService.getModel(new EngineSelection(EngineType.GROK, "grok-4-1-fast-non-reasoning")).orElseThrow();
    this.companiesModel = modelsDBService.getModel(new EngineSelection(EngineType.GROK, "grok-4-1-fast-non-reasoning")).orElseThrow();
  }

  @Override
  public AIJobSearchRequest createRequest(SearchJobOrder order) {
    AIJobSearchRequest request = super.createRequest(order);
    request.setDiscoveryModel(discoveryModel);
    request.setCompaniesModel(companiesModel);
    return request;
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.GROK;
  }

}
