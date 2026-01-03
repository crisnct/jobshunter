package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.gptRequest.Reasoning;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GptJobHunting extends GenericJobHunting<GptJobSearchRequest> {

  public GptJobHunting(
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor,
      @Qualifier("JobsClientGPT") AiJobsClient<GptJobSearchRequest, List<Job>> gptClient
  ) {
    super(gptSearchExecutor, gptClient);
  }

  @Override
  public GptJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    Reasoning reasoning = order.getEngineSelection().model().startsWith("gpt-5") ? new Reasoning("high") : null;
    return new GptJobSearchRequest(order, prompt, reasoning);
  }

  @Override
  public GptJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    return new GptJobSearchRequest(order, null, null);
  }

}
