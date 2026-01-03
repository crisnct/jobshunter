package com.jobshunter.dto.grokRequest.tools;

/**
 * Definiția unei funcții expuse către model (OpenAI-style function).
 */
public record FunctionDefinition(

    String name,

    String description,

    SearchJobsParameters parameters

) {

  public FunctionDefinition(SearchJobsParameters parameters) {
    this("search_jobs", "Search for jobs on public platforms and return relevant links", parameters);
  }
}
