package com.jobshunter.service.testdata;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.ModelSpecific;
import com.jobshunter.model.Job;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EconomyJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public final class DummyEconomyGpt extends AbstractGptApiClient implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public DummyEconomyGpt(ApplicationProperties properties, com.jobshunter.service.clients.UrlExtractor urlExtractor) {
    super(properties, urlExtractor);
  }

  @Override
  public ModelSpecific getConfig() {
    return properties.getGpt().getEconomy();
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, String userPrompt, ModelSpecific cfg, String fileId) {
    String model = getConfig().getModel();
    return List.of(
        new Job(95,
            "https://jobs.digitalhire.com/job-listing/opening/6W2b0Y7QrlHiOrwemePL8C?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply&utm_medium=organic",
            model
        ),
        new Job(72,
            "https://www.accenture.com/us-en/careers/jobdetails?id=R00298524_en&title=SAP+Intercompany+Manager+-+Life+Sciences",
            model
        ),
        new Job(71,
            "https://www.linkedin.com/jobs/collections/recommended/?currentJobId=4263267426",
            model
        )
    );
  }
}
