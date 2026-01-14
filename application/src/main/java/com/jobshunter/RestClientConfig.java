package com.jobshunter;

import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.clients.browser.RedirectFetchPage;
import com.jobshunter.service.clients.browser.BrowserSimulator;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.DnsResolver;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Centralized RestClient configuration using Apache HttpClient 5. Provides singleton beans for connection pooling, HttpClient, and RestClient
 * instances.
 *
 * <p>This configuration ensures:
 * <ul>
 *   <li>Shared connection pool across all HTTP clients</li>
 *   <li>Singleton HttpClient instance for efficient resource usage</li>
 *   <li>Separate RestClient instances for API calls (5-minute timeout) and web scraping (5-second timeout)</li>
 * </ul>
 */
@Configuration
@Slf4j
public class RestClientConfig {

  private static final int RESPONSE_TIMEOUT = 15;//minutes

  /**
   * Shared connection pool for all HTTP clients. Singleton bean to ensure efficient resource usage across the application.
   */
  @Bean
  public PoolingHttpClientConnectionManager httpClientConnectionManager() {
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT))
        .build();

    return PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig)
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(30)
        .setDnsResolver(new DnsResolver() {
          @Override
          public InetAddress[] resolve(String host) throws UnknownHostException {
            return Arrays.stream(InetAddress.getAllByName(host))
                .filter(addr -> addr instanceof Inet4Address)
                .toArray(InetAddress[]::new);
          }

          @Override
          public String resolveCanonicalHostname(String host) {
            return host;
          }
        })
        .build();
  }

  /**
   * Singleton HttpClient instance using the shared connection pool. Configured with connection eviction and redirect strategy based on application
   * properties.
   */
  @Bean
  public HttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
        .setResponseTimeout(Timeout.ofMinutes(RESPONSE_TIMEOUT)) // Default timeout, can be overridden per RestClient
        .setMaxRedirects(3)
        .setRedirectsEnabled(true)
        .build();

    HttpClientBuilder clientBuilder = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictExpiredConnections()
        .evictIdleConnections(Timeout.ofMinutes(5));

    clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());

    return clientBuilder.build();
  }

  /**
   * Default RestClient bean for general API calls. Uses a 5-minute response timeout suitable for long-running API operations.
   *
   * <p>Marked as {@link Primary} so that classes injecting {@link RestClient} without a qualifier
   * will receive this bean by default.
   */
  @Bean
  @Primary
  public RestClient restClient(HttpClient httpClient) {
    RestClient.Builder restBuilder = RestClient.builder()
        .defaultHeader(JHHeaders.ACCEPT, "application/json");

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

    requestFactory.setHttpContextFactory((request, context) -> {
      if (RedirectFetchPage.HTTP_CONTEXT.isBound()) {
        return RedirectFetchPage.HTTP_CONTEXT.get();
      }
      return HttpClientContext.create();
    });

    restBuilder.requestFactory(requestFactory);

    return restBuilder.build();
  }

  /**
   * RestClient bean specifically configured for web scraping operations. Uses a 5-second response timeout for fast-fail scenarios when scraping web
   * pages.
   *
   * <p>This bean is used by {@link BrowserSimulator} for fetching HTML content.
   */
  @Bean("webScrapingRestClient")
  public RestClient webScrapingRestClient(
      PoolingHttpClientConnectionManager connectionManager
  ) {
    // Create a separate RequestConfig with shorter timeout for web scraping
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(Timeout.ofSeconds(2))
        .setRedirectsEnabled(false)
        .setMaxRedirects(0)
        .setResponseTimeout(Timeout.ofSeconds(5)) // Fast timeout for web scraping
        .build();

    HttpClientBuilder clientBuilder = HttpClients.custom()
        .setConnectionManager(connectionManager) // Share the same connection pool
        .setDefaultRequestConfig(requestConfig)
        .disableRedirectHandling()
        .evictExpiredConnections()
        .evictIdleConnections(Timeout.ofMinutes(5));

    HttpClient httpClient = clientBuilder.build();

    RestClient.Builder restBuilder = RestClient.builder()
        .defaultHeader(JHHeaders.ACCEPT, "application/json");

    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    restBuilder.requestFactory(requestFactory);

    return restBuilder.build();
  }
}
