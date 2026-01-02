package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
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
public class SerpJobHunting extends GenericJobHunting<SearchWithSerpRequest> {

  private final JsonMapper mapper;

  public SerpJobHunting(
      @Qualifier("JobsClientSerp") AiJobsClient<SearchWithSerpRequest, List<Job>> serpApiClient,
      @Qualifier("serpApiExecutor") Executor serpApiExecutor,
      JsonMapper mapper
  ) {
    super(serpApiExecutor, serpApiClient);
    this.mapper = mapper;
  }

  @Override
  public SearchWithSerpRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    try {
      SearchWithSerpRequest request = mapper.readValue(prompt.getPrompt(), SearchWithSerpRequest.class);
      request.setOrder(order);
      request.setPrompt(prompt);
      return request;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SearchWithSerpRequest createCompaniesRequest(SearchJobOrder order) {
    return null;
  }

}
