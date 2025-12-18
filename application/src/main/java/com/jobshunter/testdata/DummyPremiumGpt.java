package com.jobshunter.testdata;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.config.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.GptJobSearchRequest;
import com.jobshunter.dto.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.PremiumGptClient;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "true")
public final class DummyPremiumGpt extends AbstractGptApiClient
    implements PremiumGptClient<GptJobSearchRequest, List<Job>> {

  @Autowired
  private ApplicationProperties properties;

  @Override
  public ModelSpecific getConfig() {
    return properties.getGpt().getPremium();
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
