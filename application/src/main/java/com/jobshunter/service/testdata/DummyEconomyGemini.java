package com.jobshunter.service.testdata;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.ApplicationProperties.ModelSpecific;
import com.jobshunter.dto.Job;
import com.jobshunter.dto.geminiRequest.GeminiJobSearchRequest;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.gemini.AbstractGeminiApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EconomyJobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "false")
public final class DummyEconomyGemini extends AbstractGeminiApiClient implements AiJobsClient<GeminiJobSearchRequest, List<Job>> {

  @Autowired
  private ApplicationProperties properties;

  @Override
  public ModelSpecific getConfig() {
    return properties.getGemini().getEconomy();
  }

  @Override
  public List<Job> searchJobs(GeminiJobSearchRequest request) {
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
