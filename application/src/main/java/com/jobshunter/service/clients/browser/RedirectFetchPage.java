package com.jobshunter.service.clients.browser;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class RedirectFetchPage implements HttpFetcher {

  public static final ScopedValue<HttpClientContext> HTTP_CONTEXT = ScopedValue.newInstance();

  public static final int TIMEOUT_REDIRECTION = 60; //sec

  private final BrowserSimulator browserSimulator;

  @Override
  public HttpFetchResult fetch(String url) {
    log.info("Checking redirection for job URL: {}", url);
    HttpClientContext httpcontext = HttpClientContext.create();
    return ScopedValue.where(HTTP_CONTEXT, httpcontext)
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

            return new HttpFetchResult(url, redirectedURL, response.getStatusCode().value(), response.getBody(), redirects, httpcontext);
          } catch (Throwable e) {
            log.error("Redirection error {} for url {}", e.getMessage(), url);
            return new HttpFetchResult(url, null, HttpStatus.NOT_FOUND.value(), null, List.of(), httpcontext);
          }
        });
  }

}
