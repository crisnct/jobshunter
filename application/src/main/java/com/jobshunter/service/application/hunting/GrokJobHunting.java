package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.dto.exceptions.ValidationException;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.EngineType;
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
public class GrokJobHunting extends AiConversationJobHunting<GrokJobSearchRequest> {

  public GrokJobHunting(
      @Qualifier("grokSearchExecutor") Executor executor,
      @Qualifier("JobsClientGROK") AiJobsClient<GrokJobSearchRequest, AiClientResponse> aiClient,
      UserCvService userCvService,
      TemplateRenderer templateRenderer,
      AiConversationStateMachine conversationStateMachine
  ) {
    super(executor, aiClient, userCvService, templateRenderer, conversationStateMachine);
  }

  @Override
  public GrokJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    UserRemoteCvEntity remoteCV = order.getUser().getRemoteCvs().stream()
        .filter(p -> p.getProvider() == EngineType.GROK).findAny()
        .orElseThrow(() -> new ValidationException("No GROK CV found for user " + order.getUser().getId()));

    GrokJobSearchRequest request = new GrokJobSearchRequest(order);
    request.setSearchCompanies(false);
    request.setPromptId(prompt.getId());
    request.setFileId(remoteCV.getFileId());
    request.setStoreConversation(true);
    request.setUserPrompt(prompt.getPrompt());
    return request;
  }

  @Override
  public GrokJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    GrokJobSearchRequest request = new GrokJobSearchRequest(order);
    request.setSearchCompanies(true);
    return request;
  }

}
