package com.jobshunter.service.application.hunting;

import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HuntingOrchestrator {

  private final Map<EngineType, JobHunting> huntingRegistry;

  public HuntingOrchestrator(List<JobHunting> huntingStrategies) {
    this.huntingRegistry = huntingStrategies.stream()
        .collect(Collectors.toUnmodifiableMap(JobHunting::getEngineType, Function.identity()));
  }

  public CompletableFuture<List<Job>> startHunting(SearchJobOrder order) {
    EngineType provider = order.getModel().getProvider();
    JobHunting hunting = huntingRegistry.get(provider);

    if (hunting == null) {
      log.error("No job hunting implementation registered for provider: {}", provider);
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<List<Job>>> allFutureJobs = new ArrayList<>();

    if (order.isSearchByUserPrompt() && hunting instanceof JobByPromptHunting promptHunting) {
      allFutureJobs.add(promptHunting.searchJobsAsync(order));
    }
    if (order.isSearchCompanies() && hunting instanceof JobByCompanyHunting companyHunting) {
      allFutureJobs.add(companyHunting.searchJobsByCompaniesAsync(order));
    }

    return CompletableFuture.allOf(allFutureJobs.toArray(CompletableFuture[]::new))
        .thenApply(_ -> allFutureJobs.stream()
            .flatMap(cf -> cf.join().stream())
            .collect(Collectors.toList())  // Mutable list
        )
        .thenApply(jobs -> removeDuplicatesBetweenSources(jobs, order.getIgnoredURLs()));
  }

  private List<Job> removeDuplicatesBetweenSources(
      List<Job> jobs,
      List<String> existingURLs) {
    Set<String> seenUrls = new HashSet<>(existingURLs);

    return jobs.stream()
        .filter(jc -> {
          String url = jc.getUrl();
          return url != null && seenUrls.add(url);
        })
        .toList();
  }

}
