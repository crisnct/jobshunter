package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import com.jobshunter.service.clients.browser.HttpFetcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
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
    String url = context.getJob().getUrl();
    HttpFetchResult result = fetcher.fetch(url);
    context.setFetchResult(result);
    if (result.statusCode() == HttpStatus.OK.value()) {
      context.setPhase(JobPhase.FETCH);
    } else {
      context.failJob("Page body can not be extracted, status code: " + result.statusCode());
    }

    return context;
  }
}
