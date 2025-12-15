package com.jobshunter.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

//@formatter:off
/**
 * RestClient configuration using Apache HttpClient 5.
 * Stable for long-running applications and external API calls.
 */
//@formatter:on
@Configuration
public class RestClientConfig {

  @Bean
  public RestClient restClient() {
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(5))
        .build();

    PoolingHttpClientConnectionManager connectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .setMaxConnTotal(100)
            .setMaxConnPerRoute(30)
            .build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofSeconds(15))
        .build();

    HttpClient httpClient = HttpClients.custom()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .evictIdleConnections(Timeout.ofSeconds(30))
        .evictExpiredConnections()
        .build();

    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    return RestClient.builder()
        .requestFactory(requestFactory)
        .defaultHeader("Accept", "application/json")
        .build();
  }
}
