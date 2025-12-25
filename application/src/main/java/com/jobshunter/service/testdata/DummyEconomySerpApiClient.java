package com.jobshunter.service.testdata;

import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.Job;
import com.jobshunter.service.clients.AiJobsClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EconomyJobsClientSerp")
@ConditionalOnProperty(name = "serpApi.enabled", havingValue = "false")
public final class DummyEconomySerpApiClient implements AiJobsClient<SearchWithSerpRequest, List<Job>> {

  @Override
  public List<Job> searchJobs(SearchWithSerpRequest request) {
    String model = "google_jobs";
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
