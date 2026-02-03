package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.browser.HtmlUtils;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class JobBodyExtractorProcessor implements JobProcessor {

  @Override
  public JobContext processAsync(JobContext context) {
    HttpFetchResult result = context.getFetchResult();
    if (context.hasFetchResult()) {
      context.setBody(HtmlUtils.cleanupHTML(result.body()));
      context.setPhase(JobPhase.BODY_EXTRACTION);
    } else {
      context.failJob("Missing body from fetch result");
    }
    return context;
  }

}
