package com.jobshunter.service.clients.ipinfo;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.IpInfoResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.IpInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("IpInfo")
@RequiredArgsConstructor
@PackageExpected("com.jobshunter.service.clients.ipinfo")
@ConditionalOnProperty(name = "ipinfo.enabled", havingValue = "true")
public non-sealed class IpInfoClient implements IpInfo {

  public static final String DEFAULT_URL = "https://api.ipinfo.io/lite/{{ip}}?token={{apikey}}";

  public static final String DETAIL_INFO_URL = "https://ipinfo.io/{{ip}}?token={{apikey}}";

  private final ApplicationProperties properties;

  private final RestClient restClient;

  @Override
  @RateLimiter(name = "ipInfoLimiter")
  @CircuitBreaker(name = "ipinfo", fallbackMethod = "fallbackIpInfo")
  public IpInfoResponse getIpDefaultInfo(String ip) {
    return restClient.get()
        .uri(DEFAULT_URL.replace("{{ip}}", ip).replace("{{apikey}}", properties.getIpInfo().getApiKey()))
        .retrieve()
        .body(IpInfoResponse.class);
  }

  @Override
  @RateLimiter(name = "ipInfoLimiter")
  @CircuitBreaker(name = "ipinfo", fallbackMethod = "fallbackIpInfoDetail")
  public IpInfoDetailResponse getIpDetailInfo(String ip) {
    return restClient.get()
        .uri(DETAIL_INFO_URL.replace("{{ip}}", ip).replace("{{apikey}}", properties.getIpInfo().getApiKey()))
        .retrieve()
        .body(IpInfoDetailResponse.class);
  }

  @SuppressWarnings("unused")
  private IpInfoResponse fallbackIpInfo(String ip, Throwable throwable) {
    return null;
  }

  @SuppressWarnings("unused")
  private IpInfoDetailResponse fallbackIpInfoDetail(String ip, Throwable throwable) {
    return null;
  }

}
