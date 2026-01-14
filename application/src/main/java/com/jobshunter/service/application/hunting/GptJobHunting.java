package com.jobshunter.service.application.hunting;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.application.processors.JobsStateMachine;
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
      TemplateRenderer templateRenderer,
      JobsStateMachine jobsStateMachine,
      ApplicationProperties applicationProperties
  ) {
    super(gptSearchExecutor, gptClient, userCvService, templateRenderer, jobsStateMachine, applicationProperties);
  }

  @Override
  public GptJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    UserRemoteCvEntity remoteCV = order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == EngineType.GPT).findAny()
        .orElseThrow(() -> new ValidationException("No GPT CV found for user " + order.getUser().getId()));

    Reasoning reasoning = order.getEngineSelection().model().startsWith("gpt-5")
        ? new Reasoning("minimal") : null;
    GptJobSearchRequest request = new GptJobSearchRequest(order, reasoning);
    request.setUserPrompt(prompt.getPrompt());
    request.setPromptId(prompt.getId());
    request.setFileId(remoteCV.getFileId());
    request.setStoreConversation(true);
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
