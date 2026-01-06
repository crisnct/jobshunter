package com.jobshunter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.clients.BrowserSimulator;
import java.nio.charset.StandardCharsets;
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
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

//@formatter:off
/**
 * RestClient configuration using Apache HttpClient 5.
 * Stable for long-running applications and external API calls.
 */
//@formatter:on
@Configuration
@Slf4j
public class ApplicationConfig {

  @Bean
  public RestClient restClient(ApplicationProperties properties) {
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
        .setResponseTimeout(Timeout.ofMinutes(5))
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

  @Bean
  public JsonMapper createMapper() {
    return JsonMapper.builder().findAndAddModules().build();
  }

  @Bean(name = "springSecurityMessageSource")
  public MessageSource springSecurityMessageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("org.springframework.security.messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name()); // use platform default to suppress ISO-8859-1 log noise
    return messageSource;
  }

  /// To be removed
  @Deprecated
  @Bean
  public RestTemplate restTemplate() {
    // request factory cu timeout-uri
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(15000);

    var restTemplate = new RestTemplate(factory);
    // interceptor exact ca în Spring Boot 3
    restTemplate.getInterceptors().add((request, body, execution) -> {
      var headers = request.getHeaders();
      headers.set(JHHeaders.USER_AGENT, "Mozilla/5.0");
      headers.set(JHHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
      headers.set(JHHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      headers.set(JHHeaders.REFERER, "https://www.jobs-hunter.com");

      return execution.execute(request, body);
    });

    return restTemplate;
  }
}
