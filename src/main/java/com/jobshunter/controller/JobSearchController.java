package com.jobshunter.controller;

import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.service.application.JobHuntOrchestrator;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job")
@RequiredArgsConstructor
@Slf4j
public class JobSearchController {

  @Autowired
  private JobHuntOrchestrator orchestrator;

  @Autowired
  private UserRepository userRepository;

  @PostMapping("/search")
  public ResponseEntity<Void> search(
      @RequestParam(name = "notifyOnWhatsupp", required = false, defaultValue = "false") Boolean notifyOnWhatsupp
  ) throws InterruptedException {
    orchestrator.searchJobsForAll(false, notifyOnWhatsupp);
    return ResponseEntity.ok(null);
  }

  @Scheduled(fixedRateString = "${jobshunter.scheduler.frequency:3600000}")
  public void scheduledRun() throws InterruptedException {
    log.info("Starts scheduled job hunt...");
    orchestrator.searchJobsForAll(true, true);
    log.info("Stop scheduled job hunt.");
  }

  @GetMapping("/status")
  public ResponseEntity<?> status() {
    return Optional.ofNullable(orchestrator.lastRun())
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.ok(Map.of("message", "No search executed yet")));
  }
}
