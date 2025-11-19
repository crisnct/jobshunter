package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.model.JobHuntSummary;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobHuntOrchestrator {

  private final ChatGptJobApiClient chatGptJobApiClient;
  private final WhatsAppNotifier whatsAppNotifier;

  private final AtomicReference<String> promptRef;
  private final AtomicReference<Path> cvPathRef;
  private final AtomicReference<JobHuntSummary> lastRun = new AtomicReference<>();

  public JobHuntOrchestrator(ApplicationProperties properties,
      ChatGptJobApiClient chatGptJobApiClient,
      WhatsAppNotifier whatsAppNotifier) {
    this.chatGptJobApiClient = chatGptJobApiClient;
    this.whatsAppNotifier = whatsAppNotifier;
    this.promptRef = new AtomicReference<>(properties.getPrompt());
    this.cvPathRef = new AtomicReference<>(Path.of(properties.getCvPath()));
  }

  @Scheduled(cron = "${jobshunter.scheduler.cron:0 0 9 * * *}")
  public void scheduledRun() throws IOException, InterruptedException {
    log.info("Running scheduled job hunt...");
    runInternal(promptRef.get(), cvPathRef.get());
  }

  public JobHuntSummary runOnce(String prompt, String cvPath) throws IOException, InterruptedException {
    promptRef.set(prompt);
    Path path = Path.of(cvPath);
    cvPathRef.set(path);
    return runInternal(prompt, path);
  }

  public JobHuntSummary lastRun() {
    return lastRun.get();
  }

  private JobHuntSummary runInternal(String prompt, Path cvPath) throws IOException, InterruptedException {
    List<String> jobs = chatGptJobApiClient.search(prompt, cvPath);
    whatsAppNotifier.send(jobs);
    JobHuntSummary summary = new JobHuntSummary(jobs);
    lastRun.set(summary);
    return summary;
  }
}
