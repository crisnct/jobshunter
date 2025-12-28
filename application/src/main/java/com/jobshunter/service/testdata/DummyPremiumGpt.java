package com.jobshunter.service.testdata;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("PremiumJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public final class DummyPremiumGpt extends AbstractGptApiClient implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public DummyPremiumGpt(ApplicationProperties properties, com.jobshunter.service.clients.UrlExtractor urlExtractor) {
    super(properties, urlExtractor);
  }

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptPremium.txt";
  }

  @Override
  public List<Job> searchWithModel(String systemPrompt, GptJobSearchRequest request) {
    String model = request.getPrompt().getEngineConfiguration().getModel();
    return List.of(
        new Job(95,
            "https://www.crossover.com/jobs/java-developer/ro/timisoara",
            model
        ),
        new Job(72,
            "https://jobs-cee.pwc.com/ro/ro/job/680253WD/Senior-Manager-Technical-Lead-Security-Risk-and-Architecture",
            model
        ),
        new Job(71,
            "https://www.wearedevelopers.com/en/companies/3227/deichmann-se/47974/senior-java-developer-springboot-m-w-d",
            model
        )
    );
  }
}
