package com.jobshunter.service.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.dto.Job;
import com.jobshunter.service.clients.BrowserSimulator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class JobHuntServiceTest {

  private Executor executor;
  private BrowserSimulator browserSimulator;
  private JobsValidator jobsValidator;

  @BeforeEach
  void setUp() {
    executor = Executors.newFixedThreadPool(4);
    browserSimulator = mock(BrowserSimulator.class);

    ApplicationProperties props = new ApplicationProperties();
    props.getJobsHunter().setExpiredExpressions("expired");

    jobsValidator = new JobsValidator(executor, props, browserSimulator);
  }

  @AfterEach
  void tearDown() {
    if (executor instanceof java.util.concurrent.ExecutorService service) {
      service.shutdownNow();
    }
  }

  @Test
  void shouldFilterExpiredJobs() {
    when(browserSimulator.getFinalRedirectedURL(any())).thenAnswer(inv -> inv.getArgument(0));
    when(browserSimulator.openPage(any())).thenReturn(ResponseEntity.ok("<html>expired</html>"));

    List<Job> jobs = List.of(
        new Job(0, "https://example.com/a", "src"),
        new Job(0, "https://example.com/b", "src")
    );

    List<Job> result = jobsValidator.validateJobs(jobs);

    org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty(), "All jobs should be filtered as expired");
  }

}
