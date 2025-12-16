package com.jobshunter.service.clients.google;

import java.util.List;

public record ParseResult(
    boolean ok,
    boolean empty,
    String jobsState,
    String error,
    List<JobHit> jobs,
    List<FilterLink> filters
) {

  static ParseResult success(List<JobHit> jobs, List<FilterLink> filters) {
    return new ParseResult(true, false, null, null, jobs, filters);
  }

  static ParseResult empty(String jobsState, List<FilterLink> filters) {
    return new ParseResult(true, true, jobsState, null, List.of(), filters);
  }

  static ParseResult error(String error, String jobsState, List<FilterLink> filters) {
    return new ParseResult(false, true, jobsState, error, List.of(), filters);
  }
}