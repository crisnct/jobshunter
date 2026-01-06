package com.jobshunter.service.clients;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.security.JHHeaders;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

  private final ApplicationProperties properties;

  private final RestClient restClient;

  private final Executor miscExecutor;

  public BrowserSimulator(
      ApplicationProperties properties,
      @Qualifier("miscellaneousExecutor") Executor miscExecutor
  ) {
    this.properties = properties;
    this.restClient = restClientFailFast(properties, 5);
    this.miscExecutor = miscExecutor;
  }

  public CompletionStage<ResponseEntity<String>> openPageAsync(String url) {
    return CompletableFuture.supplyAsync(() -> openPageSync(url), miscExecutor);
  }

  @Nonnull
  public ResponseEntity<String> openPageSync(String url) {
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.ALL)
          .header(JHHeaders.USER_AGENT, "JobsHunter" + System.currentTimeMillis() + "in64; x64)")//This is mandatory hack
          .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
          .retrieve()
          .toEntity(String.class);
    } catch (Throwable ex) {
      log.warn("First time failure about getting the html for {}", url);
      try {
        return restClient.get()
            .uri(url)
            .accept(BROWSER_ACCEPT)
            .header(JHHeaders.REFERER, "JobsHunter" + System.currentTimeMillis())
            .header(JHHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_HEADER)
            .header(JHHeaders.CONNECTION, CONNECTION_HEADER)
            .header(JHHeaders.USER_AGENT, "JobsHunter" + System.currentTimeMillis() + "in64; x64)")//This is mandatory hack
            .header(JHHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header(JHHeaders.ACCEPT_ENCODING, "gzip, deflate")
            .retrieve()
            .toEntity(String.class);
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
            openPageSync(url);

            HttpClientContext ctx = HTTP_CONTEXT.get();
            List<URI> redirects = ctx.getRedirectLocations().getAll();
            URI finalUri = redirects.isEmpty() ? URI.create(url) : redirects.getLast();

            return finalUri.toString();
          } catch (Throwable e) {
            log.error("Redirection error {} for url {}", e.getMessage(), url);
            return url;
          }
        });
  }

  private RestClient restClientFailFast(ApplicationProperties properties, int responseTimeoutSeconds) {
    // 1. Connection-level config (TCP + TLS)
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(10))
        .build();

    // 2. Connection pool (shared, bounded)
    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(30)
            .build();

    // 3. Request-level timeouts + redirect safety
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(2))          // wait for pool
        .setResponseTimeout(Timeout.ofSeconds(responseTimeoutSeconds))
        .setMaxRedirects(5)                                         // critical safety
        .build();

    // 4. HttpClient (single instance, no duplication)
    HttpClientBuilder clientBuilder = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictExpiredConnections()
        .evictIdleConnections(Timeout.ofMinutes(5));

    if (properties.getJobsHunter().getAllowRedirection()) {
      clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
    }

    HttpClient httpClient = clientBuilder.build();

    // 5. RestClient wiring
    RestClient.Builder restBuilder = RestClient.builder()
        .defaultHeader(JHHeaders.ACCEPT, "application/json");

    // 6. HttpContext propagation (for redirects, diagnostics)
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    requestFactory.setHttpContextFactory((request, context) -> {
      if (BrowserSimulator.HTTP_CONTEXT.isBound()) {
        return BrowserSimulator.HTTP_CONTEXT.get();
      }
      return HttpClientContext.create();
    });

    restBuilder.requestFactory(requestFactory);

    return restBuilder.build();
  }

}
