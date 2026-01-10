package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.GrokJobSearchRequest;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GrokJobHunting extends AdditionalEffortJobHunting<GrokJobSearchRequest> {

  public GrokJobHunting(
      @Qualifier("grokSearchExecutor") Executor executor,
      @Qualifier("JobsClientGROK") AiJobsClient<GrokJobSearchRequest, AiClientResponse> aiClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer
  ) {
    super(executor, aiClient, userCvService, templateRenderer);
  }

  @Override
  public GrokJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    GrokJobSearchRequest request
        = new GrokJobSearchRequest(order.getUser(), order.getEngineSelection());
    request.setSearchCompanies(false);
    request.setPromptId(prompt.getId());
    request.setStoreConversation(true);
    request.setUserPrompt(prompt.getPrompt());
    return request;
  }

  @Override
  public GrokJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    GrokJobSearchRequest request = new GrokJobSearchRequest(order.getUser(), order.getEngineSelection());
    request.setSearchCompanies(true);
    return request;
  }

}
