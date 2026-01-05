package com.jobshunter.dto.serpResponse;

import java.util.List;

public record SerpJobsResult(
    String id,
    List<SerpJobHit> jobs,
    String nextPageToken
) {

  public static SerpJobsResult success(String id, List<SerpJobHit> jobs, String nextPageToken) {
    return new SerpJobsResult(id, jobs, nextPageToken);
  }

  public static SerpJobsResult empty() {
    return new SerpJobsResult(null, List.of(), null);
  }

}