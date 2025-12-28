package com.jobshunter.service.testdata;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EconomyJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public final class FakeGptEconomy extends AbstractGptApiClient implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public FakeGptEconomy(ApplicationProperties properties, UrlExtractor urlExtractor) {
    super(properties, urlExtractor);
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, GptJobSearchRequest request) {
    return List.of(
        new Job(-1,
            "https://br.bebee.com/job/63c331e10c2e5c04df61d25ef8219be8?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic",
            null
        ),
        new Job(-1,
            "https://www.dice.com/job-detail/1f3c5759-dfad-40d7-9e0a-aa6fdd24db5c?utm_source=openai",
            null
        ),
        new Job(-1,
            "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=42955246261",
            null
        ),
        new Job(-1,
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa",
            null
        ),
        new Job(-1,
            "https://weworkremotely.com/remote-jobs/h2corporation-vice-president-of-engineering-usa",
            null
        )
    );
  }

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptEconomy.txt";
  }
}
