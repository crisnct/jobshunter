package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SerpJobHunting extends GenericJobHunting<SearchWithSerpRequest> {

  private final ApplicationProperties properties;

  private final JsonMapper mapper;

  public SerpJobHunting(
      @Qualifier("EconomyJobsClientSerp") AiJobsClient<SearchWithSerpRequest, List<Job>> serpApiClient,
      @Qualifier("serpApiExecutor") Executor serpApiExecutor,
      JsonMapper mapper,
      ApplicationProperties properties
  ) {
    super(serpApiExecutor, serpApiClient, null);
    this.mapper = mapper;
    this.properties = properties;
  }

  @Override
  public SearchWithSerpRequest createRequest(UserEntity user, UserPromptEntity prompt) {
    try {
      SearchWithSerpRequest request = mapper.readValue(prompt.getPrompt(), SearchWithSerpRequest.class);
      request.setUsername(user.getUsername());
      request.setPrompt(prompt);
      return request;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
