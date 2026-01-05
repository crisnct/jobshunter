package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.grokRequest.Reasoning;
import com.jobshunter.model.GrokJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GrokJobHunting extends GenericJobHunting<GrokJobSearchRequest> {

  public GrokJobHunting(
      @Qualifier("grokSearchExecutor") Executor executor,
      @Qualifier("JobsClientGROK") AiJobsClient<GrokJobSearchRequest, List<Job>> aiClient,
      UserCvService userCvService
  ) {
    super(executor, aiClient, userCvService);
  }

  @Override
  public GrokJobSearchRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    return new GrokJobSearchRequest(order, prompt, new Reasoning("high"));
  }

  @Override
  public GrokJobSearchRequest createCompaniesRequest(SearchJobOrder order) {
    return new GrokJobSearchRequest(order, null, null);
  }

}
