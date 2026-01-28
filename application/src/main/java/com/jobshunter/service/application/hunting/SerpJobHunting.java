package com.jobshunter.service.application.hunting;

import com.jobshunter.model.EngineType;
import com.jobshunter.service.application.UserCvService;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class SerpJobHunting extends GenericJobHunting {

  public SerpJobHunting(
      @Qualifier("JobsClientSerp") AiJobsClient serpClient,
      @Qualifier("serpExecutor") Executor serpExecutor,
      UserCvService userCvService,
      CountryIsoCode countryIsoCode
  ) {
    super(serpExecutor, serpClient, countryIsoCode, userCvService);
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.SERP;
  }

}
