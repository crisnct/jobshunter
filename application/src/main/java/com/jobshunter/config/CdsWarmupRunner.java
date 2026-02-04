package com.jobshunter.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * When {@code app.cds.warmup=true}, starts the Spring context to load application classes for AppCDS generation, then exits cleanly so the JVM can
 * write the archive.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.cds.warmup", havingValue = "true")
public class CdsWarmupRunner implements ApplicationRunner {

  @Override
  public void run(ApplicationArguments args) {
    log.info("AppCDS warmup completed; exiting to write archive.");
    System.exit(0);
  }
}

