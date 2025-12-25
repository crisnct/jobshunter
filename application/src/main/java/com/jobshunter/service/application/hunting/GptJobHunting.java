package com.jobshunter.service.application.hunting;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GptJobHunting extends GenericJobHunting<GptJobSearchRequest> {

  private final ApplicationProperties properties;

  public GptJobHunting(
      @Qualifier("gptSearchExecutor") Executor gptSearchExecutor,
      @Qualifier("EconomyJobsClientGPT") AiJobsClient<GptJobSearchRequest, List<Job>> gptEconomy,
      @Qualifier("PremiumJobsClientGPT") AiJobsClient<GptJobSearchRequest, List<Job>> gptPremium,
      UserDataService userDataService,
      ApplicationProperties properties
  ) {
    super(gptSearchExecutor, gptEconomy, gptPremium, userDataService);
    this.properties = properties;
  }

  @Override
  public GptJobSearchRequest createRequest(UserEntity user, UserPromptEntity prompt) {
    String gptFileId = user.getCv().getGptFileId();
    return new GptJobSearchRequest(
        user.getUsername(),
        prompt,
        gptFileId
    );
  }

  @Override
  public long getDelayTaskExecution() {
    return properties.getGpt().getDelay();
  }

}
