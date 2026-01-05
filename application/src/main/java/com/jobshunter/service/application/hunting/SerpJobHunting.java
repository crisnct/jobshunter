package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SerpJobHunting extends GenericJobHunting<SearchWithSerpRequest> {

  private final JsonMapper mapper;

  public SerpJobHunting(
      @Qualifier("JobsClientSerp") AiJobsClient<SearchWithSerpRequest, AiClientResponse> serpClient,
      @Qualifier("serpExecutor") Executor serpExecutor,
      JsonMapper mapper,
      UserCvService userCvService
  ) {
    super(serpExecutor, serpClient, userCvService);
    this.mapper = mapper;
  }

  @Override
  public SearchWithSerpRequest createRequest(SearchJobOrder order, UserPromptEntity prompt) {
    try {
      SearchWithSerpRequest request = mapper.readValue(prompt.getPrompt(), SearchWithSerpRequest.class);
      request.setUser(order.getUser());
      request.setPromptId(prompt.getId());
      request.setSearchCompanies(false);
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
