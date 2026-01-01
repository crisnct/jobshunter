package com.jobshunter.service.clients;

import com.jobshunter.ApplicationProperties;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
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

  @TimeLimiter(name = "browserSimulatorLimiter")
  public CompletionStage<ResponseEntity<String>> openPage(String url) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return restClient.get()
            .uri(url)
            .accept(MediaType.ALL)
            .header("User-Agent", "JobsHunter" + System.currentTimeMillis() + "in64; x64)")
            .header("Accept-Language", ACCEPT_LANGUAGE_HEADER)
            .retrieve()
            .toEntity(String.class);
      } catch (Throwable ex) {
        log.error(
            """
                First time failure about getting the html
            """
        );
        return restClient.get()
            .uri(url)
            .accept(BROWSER_ACCEPT)
            .header("Referer", "JobsHunter" + System.currentTimeMillis())
            .header("Accept-Language", ACCEPT_LANGUAGE_HEADER)
            .header("Connection", CONNECTION_HEADER)
            .header("User-Agent", "JobsHunter" + System.currentTimeMillis() + "in64; x64)")//This is mandatory hack
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Accept-Encoding", "gzip, deflate")
            .header("Connection", "keep-alive")
            .retrieve()
            .toEntity(String.class);
      }
    });
  }

  public String getFinalRedirectedURL(@NotNull String url) {
    if (properties.getJobsHunter().getAllowRedirection()) {
      return ScopedValue.where(HTTP_CONTEXT, HttpClientContext.create())
          .call(() -> {
            try {
              this.openPage(url).toCompletableFuture().get();

              HttpClientContext context = HTTP_CONTEXT.get();
              URI finalUri =
                  context.getRedirectLocations() == null || context.getRedirectLocations().size() == 0
                      ? context.getRequest().getUri()
                      : context.getRedirectLocations().get(context.getRedirectLocations().size() - 1);

              return finalUri.toString();
            } catch (Throwable e) {
              log.error("Redirection error for url {}: {}", url, e.getMessage());
              return url;
            }
          });
    } else {
      return url;
    }
  }

}
