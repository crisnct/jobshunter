package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.GptJobSearchRequest;
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
public class GptJobHunting extends AdditionalEffortJobHunting<GptJobSearchRequest> {

  public GptJobHunting(
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor,
      @Qualifier("JobsClientGPT") AiJobsClient<GptJobSearchRequest, AiClientResponse> gptClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer
  ) {
    super(gptSearchExecutor, gptClient, userCvService, templateRenderer);
  }

  @Override
  public GptJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    Reasoning reasoning = order.getEngineSelection().model().startsWith("gpt-5")
        ? new Reasoning("minimal") : null;
    GptJobSearchRequest request = new GptJobSearchRequest(order, reasoning);
    request.setUserPrompt(prompt.getPrompt());
    request.setPromptId(prompt.getId());
    request.setStoreConversation(false);
    request.setSearchCompanies(false);
    return request;
  }

  @Override
  public GptJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    GptJobSearchRequest request = new GptJobSearchRequest(order, null);
    request.setSearchCompanies(true);
    return request;
  }

}
