package com.jobshunter.service.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class BrowserSimulator {

  public static final ScopedValue<HttpClientContext> HTTP_CONTEXT = ScopedValue.newInstance();

  private static final Pattern HOST_PATTERN = Pattern.compile("(?i)^(?:https?://)?([^/:?#]+)");

  private static final MediaType[] BROWSER_ACCEPT = {
      MediaType.TEXT_HTML,
      MediaType.APPLICATION_XHTML_XML,
      MediaType.APPLICATION_XML,
      MediaType.ALL
  };

  private static final String BROWSER_UA =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/122.0.0.0 Safari/537.36";

  private static final String ACCEPT_LANGUAGE_HEADER = "en-US,en;q=0.9";

  private static final String CONNECTION_HEADER = "keep-alive";

  @Autowired
  private RestClient restClient;

  public ResponseEntity<String> openPage(String url) {
    try {
      return restClient.get()
          .uri(url)
          .accept(MediaType.ALL)
          .header("Referer", extractHost(url))
          .header("User-Agent", BROWSER_UA)
          .header("Accept-Language", ACCEPT_LANGUAGE_HEADER)
          .header("Connection", CONNECTION_HEADER)
          .retrieve()
          .toEntity(String.class);
    } catch (HttpClientErrorException.MethodNotAllowed | HttpClientErrorException.NotAcceptable ex) {
      return restClient.get()
          .uri(url)
          .accept(BROWSER_ACCEPT)
          .header("Referer", extractHost(url))
          .header("User-Agent", BROWSER_UA)
          .header("Accept-Language", ACCEPT_LANGUAGE_HEADER)
          .header("Connection", CONNECTION_HEADER)
          .retrieve()
          .toEntity(String.class);
    }
  }

  public String extractHost(@NotBlank String url) {
    Matcher matcher = HOST_PATTERN.matcher(url.trim());
    String host = matcher.find() ? matcher.group(1) : null;
    if (host == null) {
      return url;
    } else {
      return "https://" + host;
    }
  }

  public String getFinalRedirectedURL(@NotNull String url) {
    return ScopedValue.where(HTTP_CONTEXT, HttpClientContext.create())
        .call(() -> {
          try {
            this.openPage(url);

            HttpClientContext context = HTTP_CONTEXT.get();
            URI finalUri =
                context.getRedirectLocations() == null || context.getRedirectLocations().size() == 0
                    ? context.getRequest().getUri()
                    : context.getRedirectLocations().get(context.getRedirectLocations().size() - 1);

            return finalUri.toString();
          } catch (Exception e) {
            log.error("Redirection error for url {}", url);
            return url;
          }
        });
  }

}
