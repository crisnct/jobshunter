package com.jobshunter.service.testdata;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.GptJobSearchRequest;
import com.jobshunter.model.Job;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.clients.AiJobsClient;
import com.jobshunter.service.clients.gpt.AbstractGptApiClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("PremiumJobsClientGPT")
@PackageExpected("com.jobshunter.service.clients.gpt")
@ConditionalOnProperty(name = "gpt.enabled", havingValue = "false")
public non-sealed class FakeGptPremium extends AbstractGptApiClient implements AiJobsClient<GptJobSearchRequest, List<Job>> {

  public FakeGptPremium(ApplicationProperties properties, UrlExtractor urlExtractor) {
    super(properties, urlExtractor);
  }

  @Override
  public String getSystemPromptFilename() {
    return "jobsSystemPromptPremium.txt";
  }

  @Override
  @RateLimiter(name = "gptLimiter")
  @CircuitBreaker(name = "gptCircuitBreaker", fallbackMethod = "fallbackSearch")
  public List<Job> searchJobs(GptJobSearchRequest request) {
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

  @SuppressWarnings("unused")
  private List<Job> fallbackSearch(GptJobSearchRequest request, Throwable t) {
    log.error("{} call short-circuited/bulkheaded: {}", getClass().getSimpleName(), t.getMessage());
    return List.of();
  }
}
