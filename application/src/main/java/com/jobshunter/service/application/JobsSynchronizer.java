package com.jobshunter.service.application;

import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class JobsSynchronizer {

  private final List<Job> jobs = Collections.synchronizedList(new ArrayList<>());

  private final Set<String> existingUrls = new ConcurrentSkipListSet<>();

  public JobsSynchronizer(Collection<String> existingUrls) {
    this.existingUrls.addAll(existingUrls);
  }

  public void addJobs(Collection<Job> newJobs, EngineType engine) {
    AtomicInteger counter = new AtomicInteger(0);
    newJobs.stream()
        .filter(job -> !existingUrls.contains(job.getUrl()))
        .forEach(job -> {
          job.setSource(engine.name());
          jobs.add(job);
          existingUrls.add(job.getUrl());
          counter.addAndGet(1);
        });
    log.info("{} found {} url's and are going to be validated", engine.name(), counter.get());
  }

}
