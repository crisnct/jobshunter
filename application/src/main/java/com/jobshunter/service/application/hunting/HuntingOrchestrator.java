package com.jobshunter.service.application.hunting;

import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class HuntingOrchestrator {

  private final SerpJobHunting serpJobHunting;

  private final GptJobHunting gptJobHunting;

  private final GeminiJobHunting geminiJobHunting;

  public CompletableFuture<List<Job>> startHunting(SearchJobOrder order, List<String> existingURLs) {
    List<CompletableFuture<List<Job>>> allFutureJobs = new ArrayList<>();
    allFutureJobs.add(switch (order.engineSelection().type()) {
      case GPT -> gptJobHunting.searchJobsAsync(new SearchJobOrder(order.user(), false, order.engineSelection()));
      case GEMINI -> geminiJobHunting.searchJobsAsync(new SearchJobOrder(order.user(), false, order.engineSelection()));
      case SERP -> serpJobHunting.searchJobsAsync(new SearchJobOrder(order.user(), false, order.engineSelection()));
    });
    if (order.searchCompanies()) {
      allFutureJobs.add(gptJobHunting.searchJobsByCompaniesAsync(order));
    }

    return CompletableFuture.allOf(allFutureJobs.toArray(CompletableFuture[]::new))
        .thenApply(_ -> allFutureJobs.stream()
            .flatMap(cf -> cf.join().stream())
            .collect(Collectors.toList())  // Mutable list
        )
        .thenApply(jobs -> removeDuplicatesBetweenSources(jobs, existingURLs));
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
