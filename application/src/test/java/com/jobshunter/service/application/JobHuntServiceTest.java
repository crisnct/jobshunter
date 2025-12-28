package com.jobshunter.service.application;

import static org.mockito.Mockito.mock;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.service.application.processors.JobsValidator;
import com.jobshunter.service.clients.BrowserSimulator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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

    jobsValidator = new JobsValidator(props, browserSimulator);
  }

  @AfterEach
  void tearDown() {
    if (executor instanceof java.util.concurrent.ExecutorService service) {
      service.shutdownNow();
    }
  }

}
