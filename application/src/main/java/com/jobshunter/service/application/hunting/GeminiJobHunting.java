package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.GeminiJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GeminiJobHunting extends GenericJobHunting<GeminiJobSearchRequest> {

  public GeminiJobHunting(
      @Qualifier("geminiSearchExecutor") Executor geminiSearchExecutor,
      @Qualifier("JobsClientGemini") AiJobsClient<GeminiJobSearchRequest, List<Job>> geminiClient
      ) {
    super(geminiSearchExecutor, geminiClient);
  }

  @Override
  public GeminiJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    String userCVBase64 = Base64.getEncoder().encodeToString(order.getUser().getCv().getCv());
    return new GeminiJobSearchRequest(order, prompt, userCVBase64);
  }

  @Override
  public GeminiJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    return null;
  }

}
