package com.jobshunter.service.application.hunting;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.GeminiJobSearchRequest;
import com.jobshunter.model.Job;
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
      @Qualifier("EconomyJobsClientGemini") AiJobsClient<GeminiJobSearchRequest, List<Job>> geminiEconomy,
      @Qualifier("PremiumJobsClientGemini") AiJobsClient<GeminiJobSearchRequest, List<Job>> geminiPremium) {
    super(geminiSearchExecutor, geminiEconomy, geminiPremium);
  }

  @Override
  public GeminiJobSearchRequest createRequest(UserEntity user, UserPromptEntity prompt) {
    String userCVBase64 = Base64.getEncoder().encodeToString(user.getCv().getCv());
    return new GeminiJobSearchRequest(
        user.getUsername(),
        prompt,
        userCVBase64
    );
  }

}
