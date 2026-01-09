package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class JobBodyExtractorProcessor implements JobProcessor {

  @Override
  public JobContext processAsync(JobContext context) {
    HttpFetchResult result = context.getFetchResult();
    if (!context.hasFetchResult()) {
      return context;
    }

    context.setBody(this.cleanupHTML(result.body()));
    context.setPhase(JobPhase.GETBODY);

    return context;
  }

  private String cleanupHTML(String body) {
    Document document = Jsoup.parse(body);
    document.select("script, style, nav, footer, header, aside").remove();
    document.select("button, a").remove();
    return document.text();
  }

}
