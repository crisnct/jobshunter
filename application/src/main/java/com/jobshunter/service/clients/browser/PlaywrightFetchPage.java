package com.jobshunter.service.clients.browser;

import com.microsoft.playwright.options.WaitUntilState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PlaywrightFetchPage implements HttpFetcher {

  private final BrowserSimulator browserSimulator;

  @Override
  public HttpFetchResult fetch(String url) {
    log.info("Checking redirection for job URL: {}", url);
    HttpClientContext httpcontext = HttpClientContext.create();
    try {
      log.info("Fetching page content with Playwright {}", url);
      String body = browserSimulator.openPageSyncPlaywright(url, WaitUntilState.LOAD).getBody();
      return new HttpFetchResult(HttpStatus.OK.value(), body, httpcontext);
    } catch (Throwable e) {
      log.error("Playwright error {} for url {}", e.getMessage(), url);
      log.error("Playwright error", e);
      return new HttpFetchResult(HttpStatus.BAD_REQUEST.value(), "Error trying to get content for page " + url + "\n" + e.getMessage(), httpcontext);
    }
  }

}
