package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
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
public class GptJobHunting extends AiConversationJobHunting<GptJobSearchRequest> {

  public GptJobHunting(
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor,
      @Qualifier("JobsClientGPT") AiJobsClient<GptJobSearchRequest, AiClientResponse> gptClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      AiConversationStateMachine conversationStateMachine
  ) {
    super(gptSearchExecutor, gptClient, userCvService, templateRenderer, conversationStateMachine);
  }

  @Override
  public GptJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    UserRemoteCvEntity remoteCV = order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == EngineType.GPT).findAny()
        .orElseThrow(() -> new ValidationException("No GPT CV found for user " + order.getUser().getId()));

    GptJobSearchRequest request = new GptJobSearchRequest(order);
    request.setUserPrompt(prompt.getPrompt());
    request.setPromptId(prompt.getId());
    request.setFileId(remoteCV.getFileId());
    request.setStoreConversation(true);
    request.setSearchCompanies(false);
    return request;
  }

  @Override
  public GptJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    GptJobSearchRequest request = new GptJobSearchRequest(order);
    request.setSearchCompanies(true);
    return request;
  }

}
