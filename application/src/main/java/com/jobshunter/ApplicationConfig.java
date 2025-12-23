package com.jobshunter;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
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
public class ApplicationConfig {

  @Bean
  public RestClient restClient() {
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(20))
        .build();

    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(30)
            .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMinutes(5))
        .build();

    HttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictIdleConnections(Timeout.ofMinutes(5))
        .evictExpiredConnections()
        .build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

    return RestClient.builder()
        .requestFactory(requestFactory)
        .defaultHeader("Accept", "application/json")
        .build();
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
      headers.set("User-Agent", "Mozilla/5.0");
      headers.set("Accept-Language", "en-US,en;q=0.9");
      headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
      headers.set("Referer", "https://www.jobs-hunter.com");

      return execution.execute(request, body);
    });

    return restTemplate;
  }
}
