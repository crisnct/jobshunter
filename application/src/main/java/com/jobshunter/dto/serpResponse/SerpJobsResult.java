package com.jobshunter.dto.serpResponse;

import java.util.List;

public record SerpJobsResult(
    List<SerpJobHit> jobs,
    String nextPageToken
) {

  public static SerpJobsResult success(List<SerpJobHit> jobs, String nextPageToken) {
    return new SerpJobsResult(jobs, nextPageToken);
  }

  public static SerpJobsResult empty() {
    return new SerpJobsResult(List.of(), null);
  }

}