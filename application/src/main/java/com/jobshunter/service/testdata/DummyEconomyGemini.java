package com.jobshunter.service.testdata;

import com.jobshunter.model.GeminiJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.UrlExtractor;
import com.jobshunter.service.clients.gemini.AbstractGeminiApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("EconomyJobsClientGemini")
@PackageExpected("com.jobshunter.service.clients.gemini")
@ConditionalOnProperty(name = "gemini.enabled", havingValue = "false")
public final class DummyEconomyGemini extends AbstractGeminiApiClient implements AiJobsClient<GeminiJobSearchRequest, List<Job>> {

  public DummyEconomyGemini(UrlExtractor urlExtractor) {
    super(urlExtractor);
  }

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptEconomy.txt";
  }

  @Override
  public List<Job> searchJobs(GeminiJobSearchRequest request) {
    return List.of(
        new Job(-1,
            "https://devjob.ro/en/jobs/Evantage-Soft-SRL-Senior-Java-Full-Stack-Developer",
            null
        ),
        new Job(-1,
            "https://jobs.citi.com/job/pune/java-spark-senior-lead-developer-java-spark-hdfs-hive-vice-president/287/88303699072",
            null
        ),
        new Job(-1,
            "https://jobs.citi.com/job/dublin/technical-java-lead-vice-president/287/89302337248",
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
}
