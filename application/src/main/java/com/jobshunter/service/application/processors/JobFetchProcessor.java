package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import com.jobshunter.service.clients.browser.HttpFetcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public final class JobFetchProcessor implements JobProcessor {

  private final HttpFetcher fetcher;

  @Override
  public JobContext processAsync(JobContext context) {
    if (context.hasFetchResult()) {
      return context;
    }

    HttpFetchResult result = fetcher.fetch(context.getJob().getUrl());

    context.setFetchResult(result);
    context.getJob().setUrl(result.finalUrl());
    context.setPhase(JobPhase.FETCHED);

    return context;
  }
}
