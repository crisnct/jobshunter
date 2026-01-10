package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.GeminiJobSearchRequest;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.Base64;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeminiJobHunting extends GenericJobHunting<GeminiJobSearchRequest> {

  public GeminiJobHunting(
      @Qualifier("geminiSearchExecutor") Executor geminiSearchExecutor,
      @Qualifier("JobsClientGemini") AiJobsClient<GeminiJobSearchRequest, AiClientResponse> geminiClient,
      UserCvService userCvService
  ) {
    super(geminiSearchExecutor, geminiClient, userCvService);
  }

  @Override
  public GeminiJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    String userCVBase64 = Base64.getEncoder().encodeToString(order.getUser().getCv().getByteArray());
    GeminiJobSearchRequest request = new GeminiJobSearchRequest(order.getUser(), order.getEngineSelection(), userCVBase64);
    request.setPromptId(prompt.getId());
    request.setUserPrompt(prompt.getPrompt());
    return request;
  }

  @Override
  public GeminiJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    return null;
  }

}
