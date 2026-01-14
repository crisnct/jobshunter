package com.jobshunter.model;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AiClientResponse {

  private final List<Job> jobs = new ArrayList<>();

  private String id;

  public void add(Job job) {
    this.jobs.add(job);
  }

  public void addAll(List<Job> jobs) {
    this.jobs.addAll(jobs);
  }

  public void addAll(AiClientResponse anotherResponse) {
    jobs.addAll(anotherResponse.getJobs());
  }

  public boolean contains(@NotNull String url) {
    return jobs.stream().anyMatch(p -> p.getUrl().equals(url));
  }
}
