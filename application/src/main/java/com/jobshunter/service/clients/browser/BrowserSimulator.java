package com.jobshunter.service.clients.browser;

import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.UrlAffinityExecutor;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.Nonnull;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BrowserSimulator {

  private static final MediaType[] BROWSER_ACCEPT = {
      MediaType.TEXT_HTML,
      MediaType.APPLICATION_XHTML_XML,
      MediaType.APPLICATION_XML,
      MediaType.ALL
  };

  private static final String ACCEPT_LANGUAGE_HEADER = "en-US,en;q=0.9";

  private static final String CONNECTION_HEADER = "keep-alive";

  public static final int TIMEOUT = 20; //sec

  private final RestClient restClientNoRedirection;

  private final RestClient restClientWithRedirection;

  private final UrlAffinityExecutor executor;

  private final PlaywrightManager playwrightManager;

  private final Executor playwrightExecutor;

  public BrowserSimulator(
      @Qualifier("restClient") RestClient restClientWithRedirection,
      @Qualifier("webScrapingRestClient") RestClient restClientNoRedirection,
      @Qualifier("urlFetchPlaywrightExecutor") Executor playwrightExecutor,
      UrlAffinityExecutor executor,
      PlaywrightManager playwrightManager
  ) {
    this.restClientNoRedirection = restClientNoRedirection;
    this.restClientWithRedirection = restClientWithRedirection;
    this.executor = executor;
    this.playwrightExecutor = playwrightExecutor;
    this.playwrightManager = playwrightManager;
  }

  public CompletionStage<ResponseEntity<String>> openPageAsync(String url) {
    return openPageAsync(url, restClientNoRedirection);
  }

  public CompletionStage<ResponseEntity<String>> openPageAsyncRedirect(String url) {
    return openPageAsync(url, restClientWithRedirection);
  }

  private CompletionStage<ResponseEntity<String>> openPageAsync(String url, RestClient restClient) {
    return executor.submit(url, () -> openPageSyncRestClient(url, restClient))
        .orTimeout(TIMEOUT, TimeUnit.SECONDS)
        .handle((response, error) -> {
          if (error == null && response != null && response.getStatusCode().is2xxSuccessful()) {
            return CompletableFuture.completedFuture(response);
          }
          log.warn("HTTP fetch failed for {}, trying Playwright", url);
          return CompletableFuture.supplyAsync(() -> openPageSyncPlaywright(url), playwrightExecutor)
              .orTimeout(TIMEOUT, TimeUnit.SECONDS);
        }).thenCompose(Function.identity());
  }

  @Nonnull
  private ResponseEntity<String> openPageSyncRestClient(String url, RestClient restClient) {
    try {
      log.info("Getting the body of page {}", url);
      ResponseEntity<String> entity = restClient.get()
          .uri(url)
          .accept(BROWSER_ACCEPT)
          .header(JHHeaders.REFERER, URI.create(url).getHost())
          .header(JHHeaders.USER_AGENT, RandomBrowserUserAgent.pick())
          .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
          .retrieve()
          .toEntity(String.class);
      log.info("Successfully body extraction for {}", url);
      return entity;
    } catch (Throwable ex) {
      log.warn("First time failure about getting the html for {}", url);
      try {
        ResponseEntity<String> entity = restClient.get()
            .uri(url)
            .accept(BROWSER_ACCEPT)
            .header(JHHeaders.REFERER, URI.create(url).getHost())
            .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
            .header(JHHeaders.CONNECTION, CONNECTION_HEADER)
            .header(JHHeaders.USER_AGENT, "J" + System.currentTimeMillis() + "in64; x64)") //adding timestamp is mandatory hack
            .header(JHHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header(JHHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .retrieve()
            .toEntity(String.class);
        log.info("\uD83D\uDCA5 BINGO HACK WORKED. Second time worked about getting the html for {}", url);
        return entity;
      } catch (Throwable ex2) {
        log.warn("SECOND time failure about getting the html for {}", url);
        throw ex2;
      }
    }
  }

  public ResponseEntity<String> openPageSyncPlaywright(String url) {
    try (BrowserContext context = playwrightManager.newContext();
        Page page = context.newPage()) {
      try {
        navigate(url, page);
      } catch (PlaywrightException e) {
        if (e.getMessage().contains("ERR_HTTP2_PROTOCOL_ERROR")) {
          log.warn("Retrying navigation without HTTP/2");
          navigate(url, page);
        } else {
          throw e;
        }
      }
      return  ResponseEntity.ok(page.content());
    }
  }

  private void navigate(String url, Page page) {
    page.navigate(
        url,
        new Page.NavigateOptions()
            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
            .setTimeout(TimeUnit.SECONDS.toMillis(TIMEOUT))
    );
    log.info("▶️ PLAYWRIGHT successfully got the body of page {}", url);
  }

}
