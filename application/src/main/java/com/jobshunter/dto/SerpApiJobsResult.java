package com.jobshunter.dto;

import java.util.List;

public record SerpApiJobsResult(
    List<SerpApiJobHit> jobs,
    String nextPageToken
) {

  public static SerpApiJobsResult success(List<SerpApiJobHit> jobs, String nextPageToken) {
    return new SerpApiJobsResult(jobs, nextPageToken);
  }

  public static SerpApiJobsResult empty() {
    return new SerpApiJobsResult(List.of(), null);
  }

}