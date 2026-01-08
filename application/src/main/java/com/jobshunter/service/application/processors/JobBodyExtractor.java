package com.jobshunter.service.application.processors;

import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import com.jobshunter.model.UrlInfo;
import com.jobshunter.service.clients.browser.BrowserSimulator;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class JobBodyExtractor implements JobProcessor {

  public static final ScopedValue<HttpClientContext> HTTP_CONTEXT = ScopedValue.newInstance();

  public static final int TIMEOUT_REDIRECTION = 60; //sec

  private final BrowserSimulator browserSimulator;

  @Override
  public JobContext processAsync(JobContext context) {
    if (context.isRealUrl() && !context.isAccepted()) {
      log.info("Checking redirection for job URL: {}", context.getJob().getUrl());
      UrlInfo info = getURLInfo(context.getJob().getUrl());
      context.getJob().setUrl(info.getUrl());
      context.setRawBody(info.getRawBody());
    }
    context.setPhase(JobPhase.GETBODY);
    return context;
  }

  private UrlInfo getURLInfo(@NotNull String url) {
    return ScopedValue.where(HTTP_CONTEXT, HttpClientContext.create())
        .call(() -> {
          try {
            ResponseEntity<String> response = browserSimulator.openPageAsyncRedirect(url)
                .toCompletableFuture()
                .orTimeout(TIMEOUT_REDIRECTION, TimeUnit.SECONDS)
                .join();

            HttpClientContext ctx = HTTP_CONTEXT.get();
            List<URI> redirects = ctx.getRedirectLocations().getAll();
            URI finalUri = redirects.isEmpty() ? URI.create(url) : redirects.getLast();
            String redirectedURL = finalUri.toString();
            if (!redirectedURL.equals(url)) {
              log.info("Redirected(code {}) from {} to {}", response.getStatusCode().value(), url, redirectedURL);
            }
            return new UrlInfo(url, redirectedURL, response.getBody());
          } catch (Throwable e) {
            log.error("Redirection error {} for url {}", e.getMessage(), url);
            return new UrlInfo(url);
          }
        });
  }

}
