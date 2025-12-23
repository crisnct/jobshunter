package com.jobshunter.service.application;

import com.jobshunter.dto.Job;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.Data;

@Data
public class JobsSynchronizer {

  private final List<Job> jobs = Collections.synchronizedList(new ArrayList<>());

  private final Set<String> existingUrls = new ConcurrentSkipListSet<>();

  private final ValidateJobUrl jobUrlValidator;

  public JobsSynchronizer(Collection<String> existingUrls, ValidateJobUrl jobUrlValidator) {
    this.existingUrls.addAll(existingUrls);
    this.jobUrlValidator = jobUrlValidator;
  }

  public void addJobs(Collection<Job> newJobs) {
    newJobs.stream()
        .filter(job -> !existingUrls.contains(job.url()))
        .filter(job -> jobUrlValidator.isValidJob(job.url()))
        .forEach(job -> {
          jobs.add(job);
          existingUrls.add(job.url());
        });
  }

}
