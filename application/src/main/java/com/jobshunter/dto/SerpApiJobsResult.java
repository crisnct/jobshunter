package com.jobshunter.dto;

import java.util.List;

public record SerpApiJobsResult(
    boolean ok,
    boolean empty,
    String jobsState,
    String error,
    List<SerpApiJobHit> jobs,
    List<SerpApiFilterLink> filters,
    String nextPageToken
) {

  public static SerpApiJobsResult success(List<SerpApiJobHit> jobs, List<SerpApiFilterLink> filters, String nextPageToken) {
    return new SerpApiJobsResult(true, false, null, null, jobs, filters, nextPageToken);
  }

  public static SerpApiJobsResult empty(String jobsState, List<SerpApiFilterLink> filters) {
    return new SerpApiJobsResult(true, true, jobsState, null, List.of(), filters, null);
  }

  public static SerpApiJobsResult error(String error, String jobsState, List<SerpApiFilterLink> filters) {
    return new SerpApiJobsResult(false, true, jobsState, error, List.of(), filters, null);
  }
}