package com.jobshunter.model;

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

}
