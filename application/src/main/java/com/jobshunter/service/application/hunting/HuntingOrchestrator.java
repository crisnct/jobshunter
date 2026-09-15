package com.jobshunter.service.application.hunting;

import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.progress.OrderProgressPublisher;
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

  public static final Boolean REMOVE_DUPLICATES = true;

  private final Map<EngineType, JobHunting> huntingRegistry;
  private final OrderProgressPublisher orderProgressPublisher;

  public HuntingOrchestrator(List<JobHunting> huntingStrategies, OrderProgressPublisher orderProgressPublisher) {
    this.huntingRegistry = huntingStrategies.stream()
        .collect(Collectors.toUnmodifiableMap(JobHunting::getEngineType, Function.identity()));
    this.orderProgressPublisher = orderProgressPublisher;
  }

  public CompletableFuture<List<Job>> startHunting(SearchJobOrder order) {
    Long orderId = order.getJobOrder().getId();
    EngineType provider = order.getModel().getProvider();
    orderProgressPublisher.emit(orderId, "Hunting started with provider %s".formatted(provider.name()));
    JobHunting hunting = huntingRegistry.get(provider);

    if (hunting == null) {
      orderProgressPublisher.emit(orderId, "No hunting implementation found for %s".formatted(provider.name()));
      log.error("No job hunting implementation registered for provider: {}", provider);
      return CompletableFuture.completedFuture(List.of());
    }

    List<CompletableFuture<List<Job>>> allFutureJobs = new ArrayList<>();

    if (order.isSearchByUserPrompt() && hunting instanceof JobByPromptHunting promptHunting) {
      orderProgressPublisher.emit(orderId, "Searching by prompts started");
      allFutureJobs.add(promptHunting.searchJobsAsync(order));
    }
    if (order.isSearchCompanies() && hunting instanceof JobByCompanyHunting companyHunting) {
      orderProgressPublisher.emit(orderId, "Searching by companies started");
      allFutureJobs.add(companyHunting.searchJobsByCompaniesAsync(order));
    }

    return CompletableFuture.allOf(allFutureJobs.toArray(CompletableFuture[]::new))
        .thenApply(_ -> allFutureJobs.stream()
            .flatMap(cf -> cf.join().stream())
            .collect(Collectors.toList())  // Mutable list
        )
        .thenApply(jobs -> {
          orderProgressPublisher.emit(orderId, "Hunting finished with %d raw jobs".formatted(jobs.size()));
          return removeDuplicatesBetweenSources(jobs, order.getIgnoredURLs());
        });
  }

  private List<Job> removeDuplicatesBetweenSources(
      List<Job> jobs,
      List<String> existingURLs) {
    if (REMOVE_DUPLICATES) {
      Set<String> seenUrls = new HashSet<>(existingURLs);
      return jobs.stream()
          .filter(jc -> {
            String url = jc.getUrl();
            return url != null && seenUrls.add(url);
          })
          .toList();
    } else {
      return jobs;
    }
  }

}
