package com.jobshunter.service.testdata;

import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.IpInfoResponse;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.IpInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component("IpInfo")
@PackageExpected("com.jobshunter.service.clients.ipinfo")
@ConditionalOnProperty(name = "ipinfo.enabled", havingValue = "false")
public non-sealed class FakeIpInfo implements IpInfo {

  @Override
  @RateLimiter(name = "ipInfoLimiter")
  @CircuitBreaker(name = "ipinfo", fallbackMethod = "fallbackIpInfo")
  public IpInfoResponse getIpDefaultInfo(String ip) {
    return new IpInfoResponse("Romania", "RO", "EU", "ASN8593", false);
  }

  @Override
  @RateLimiter(name = "ipInfoLimiter")
  @CircuitBreaker(name = "ipinfo", fallbackMethod = "fallbackIpInfoDetail")
  public IpInfoDetailResponse getIpDetailInfo(String ip) {
    return new IpInfoDetailResponse("Dolj County", "RO", "AS8951 Orange Romania S.A", "Craiova", "Europe/Bucharest", false);
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
