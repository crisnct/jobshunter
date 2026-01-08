package com.jobshunter.service.clients;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.UrlAffinityExecutor;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BrowserSimulator {

  public static final ScopedValue<HttpClientContext> HTTP_CONTEXT = ScopedValue.newInstance();

  private static final MediaType[] BROWSER_ACCEPT = {
      MediaType.TEXT_HTML,
      MediaType.APPLICATION_XHTML_XML,
      MediaType.APPLICATION_XML,
      MediaType.ALL
  };

  private static final String ACCEPT_LANGUAGE_HEADER = "en-US,en;q=0.9";

  private static final String CONNECTION_HEADER = "keep-alive";

  public static final int TIMEOUT = 10;

  public static final int TIMEOUT_REDIRECTION = 30;

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final UrlAffinityExecutor executor;

  private final CircularList<String> userAgents;

  public BrowserSimulator(
      ApplicationProperties properties,
      @Qualifier("webScrapingRestClient") RestClient restClient,
      UrlAffinityExecutor executor
  ) {
    this.properties = properties;
    this.restClient = restClient;
    this.executor = executor;
    this.userAgents = new CircularList<>(List.of(
        // Chrome
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",

        // Edge
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",

        // Firefox
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13.6; rv:122.0) Gecko/20100101 Firefox/122.0",
        "Mozilla/5.0 (X11; Linux x86_64; rv:122.0) Gecko/20100101 Firefox/122.0",

        // Safari macOS
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",

        // Mobile Chrome
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",

        // Mobile Safari
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
    ));
  }

  public CompletionStage<ResponseEntity<String>> openPageAsync(String url) {
    return executor.submit(url, () -> openPageSync(url))
        .orTimeout(TIMEOUT, TimeUnit.SECONDS);
  }

  @Nonnull
  public ResponseEntity<String> openPageSync(String url) {
    String userAgent = "J" + System.currentTimeMillis() + "in64; x64)";//adding timestamp is mandatory hack
    try {
      log.info("Getting the body of page {}", url);
      return restClient.get()
          .uri(url)
          .accept(MediaType.ALL)
          .header(JHHeaders.USER_AGENT, userAgent)
          .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
          .retrieve()
          .toEntity(String.class);
    } catch (Throwable ex) {
      log.warn("First time failure about getting the html for {}", url);
      try {
        ResponseEntity<String> entity = restClient.get()
            .uri(url)
            .accept(BROWSER_ACCEPT)
            .header(JHHeaders.REFERER, URI.create(url).getHost())
            .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
            .header(JHHeaders.CONNECTION, CONNECTION_HEADER)
            .header(JHHeaders.USER_AGENT, userAgent)
            .header(JHHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header(JHHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .retrieve()
            .toEntity(String.class);
        log.info("Second time worked about getting the html for {}", url);
        return entity;
      } catch (Throwable ex2) {
        log.error("SECOND time failure about getting the html for {}", url);
        return ResponseEntity.ofNullable(null);
      }
    }
  }

  public String getFinalRedirectedURL(@NotNull String url) {
    if (!properties.getJobsHunter().getAllowRedirection()) {
      return url;
    }
    return ScopedValue.where(HTTP_CONTEXT, HttpClientContext.create())
        .call(() -> {
          try {
            ResponseEntity<String> response = openPageAsync(url)
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
            return redirectedURL;
          } catch (Throwable e) {
            log.error("Redirection error {} for url {}", e.getMessage(), url);
            return url;
          }
        });
  }

}
