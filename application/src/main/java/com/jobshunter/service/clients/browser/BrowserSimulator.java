package com.jobshunter.service.clients.browser;

import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.UrlAffinityExecutor;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.Nonnull;
import java.net.URI;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BrowserSimulator {

  public static final int TIMEOUT = 20; //sec
  private static final MediaType[] BROWSER_ACCEPT = {
      MediaType.TEXT_HTML,
      MediaType.APPLICATION_XHTML_XML,
      MediaType.APPLICATION_XML,
      MediaType.ALL
  };
  private static final String ACCEPT_LANGUAGE_HEADER = "en-US,en;q=0.9";
  private static final String CONNECTION_HEADER = "keep-alive";

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

  public CompletionStage<ResponseEntity<String>> openPageAsyncRedirect(String url, HttpClientContext httpContext) {
    return openPageAsync(url, restClientWithRedirection, httpContext);
  }

  private CompletionStage<ResponseEntity<String>> openPageAsync(String url, RestClient restClient) {
    return openPageAsync(url, restClient, null);
  }

  private CompletionStage<ResponseEntity<String>> openPageAsync(String url, RestClient restClient, HttpClientContext httpContext) {
    // Pass httpContext through executor by setting ThreadLocal in executor thread
    return executor.submit(url, () -> openPageSyncRestClient(url, restClient))
        .orTimeout(TIMEOUT, TimeUnit.SECONDS)
        .handle((response, error) -> {
          if (error == null && response != null && response.getStatusCode().is2xxSuccessful()) {
            return CompletableFuture.completedFuture(response);
          }
          log.warn("HTTP fetch failed for {}, trying Playwright", url);
          return CompletableFuture.supplyAsync(() -> openPageSyncPlaywright(url, WaitUntilState.DOMCONTENTLOADED), playwrightExecutor)
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

  public ResponseEntity<String> openPageSyncPlaywright(String url, WaitUntilState state) {
    BrowserContext context;
    try {
      context = playwrightManager.borrowContext();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PlaywrightException("Interrupted while waiting for a browser context");
    }
    try {
      Page page = context.newPage();
      try {
        Response response = navigate(url, page, state);

        String finalUrl = page.url();
        int status = response != null ? response.status() : -1;

        log.info("▶ Requested URL  : {}", url);
        log.info("▶ Final URL      : {}", finalUrl);
        log.info("▶ HTTP Status    : {}", status);

        // --- HARD FAIL CONDITIONS ---

        if (status >= 400) {
          log.warn("HTTP error detected: {}", status);
          return ResponseEntity.status(status).body("");
        }

        if (!finalUrl.equalsIgnoreCase(url)) {
          log.warn("Redirect detected: {} -> {}", url, finalUrl);

          // redirect to login page detection
          if (finalUrl.contains("login") || finalUrl.contains("signin")) {
            log.warn("Redirected to login page.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("");
          }
        }

        String content = page.content();

        // --- SOFT 404 DETECTION ---
        if (isSoft404(content)) {
          log.warn("Soft 404 detected for {}", url);
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(content);
        }

        return ResponseEntity.ok(content);

      } catch (PlaywrightException e) {
        if (e.getMessage() != null &&
            e.getMessage().contains("ERR_HTTP2_PROTOCOL_ERROR")) {
          log.warn("Retrying navigation without HTTP/2");
          return retryWithoutHttp2(url, state);
        }
        throw e;
      } finally {
        page.close();
      }
    } finally {
      playwrightManager.returnContext(context);
    }
  }

  private Response navigate(String url, Page page, WaitUntilState state) {
    Response response = page.navigate(
        url,
        new Page.NavigateOptions()
            .setWaitUntil(state)
            .setTimeout(TimeUnit.SECONDS.toMillis(TIMEOUT))
    );

    String finalUrl = page.url();
    int status = response != null ? response.status() : -1;

    log.info("▶️ Requested URL: {}", url);
    log.info("▶️ Final URL: {}", finalUrl);
    log.info("▶️ HTTP Status: {}", status);

    return response;
  }

  private ResponseEntity<String> retryWithoutHttp2(String url, WaitUntilState state) {
    BrowserContext context;
    try {
      context = playwrightManager.borrowContext();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PlaywrightException("Interrupted while waiting for a browser context");
    }
    try {
      Page page = context.newPage();
      try {
        Response response = navigate(url, page, state);
        int status = response != null ? response.status() : -1;
        if (status >= 400) {
          return ResponseEntity.status(status).body("");
        }
        return ResponseEntity.ok(page.content());
      } finally {
        page.close();
      }
    } finally {
      playwrightManager.returnContext(context);
    }
  }

  private boolean isSoft404(String content) {
    if (content == null || content.isBlank()) {
      return true;
    }
    String lower = content.toLowerCase(Locale.ROOT);
    return lower.contains("page not found")
        || lower.contains("job not found")
        || lower.contains("no longer available")
        || lower.contains("404");
  }

}
