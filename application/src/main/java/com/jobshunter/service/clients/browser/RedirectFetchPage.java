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

  // ThreadLocal to pass HttpClientContext across thread boundaries (for async executor calls)
  private static final ThreadLocal<HttpClientContext> THREAD_LOCAL_CONTEXT = new ThreadLocal<>();

  public static final int TIMEOUT_REDIRECTION = 60; //sec

  private final BrowserSimulator browserSimulator;

  @Override
  public HttpFetchResult fetch(String url) {
    log.info("Checking redirection for job URL: {}", url);
    HttpClientContext httpcontext = HttpClientContext.create();
    // Set ThreadLocal before async call so it's available in executor thread
    THREAD_LOCAL_CONTEXT.set(httpcontext);
    try {
      // Pass httpContext directly to ensure it's available in executor thread
      ResponseEntity<String> response = browserSimulator.openPageAsyncRedirect(url, httpcontext)
          .toCompletableFuture()
          .orTimeout(TIMEOUT_REDIRECTION, TimeUnit.SECONDS)
          .join();

      // Read redirects from the httpcontext variable (which was used in executor thread via ThreadLocal)
      HttpClientContext ctx = THREAD_LOCAL_CONTEXT.get() != null ? THREAD_LOCAL_CONTEXT.get() : httpcontext;
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
    } finally {
      // Clean up ThreadLocal after use
      THREAD_LOCAL_CONTEXT.remove();
    }
  }
  
  /**
   * Gets the HttpClientContext from ThreadLocal. Used by RestClientConfig to access the context
   * when making HTTP requests in executor threads.
   */
  public static HttpClientContext getThreadLocalContext() {
    return THREAD_LOCAL_CONTEXT.get();
  }
  
  /**
   * Sets the HttpClientContext in ThreadLocal. Used by BrowserSimulator to set context in executor thread.
   */
  public static void setThreadLocalContext(HttpClientContext context) {
    THREAD_LOCAL_CONTEXT.set(context);
  }
  
  /**
   * Removes the HttpClientContext from ThreadLocal. Used for cleanup.
   */
  public static void removeThreadLocalContext() {
    THREAD_LOCAL_CONTEXT.remove();
  }

}
