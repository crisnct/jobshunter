package com.jobshunter.service.application.processors;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobFakelUrFilter implements JobProcessor {

  private final Set<String> whitelistDomains;

  public JobFakelUrFilter(ApplicationProperties properties) {
    this.whitelistDomains = Arrays.stream(properties.getJobsHunter().getBlacklist().split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public JobContext processAsync(JobContext context) {
    log.info("Checking reachability for URL: {}", StringUtils.abbreviate(context.getJob().getUrl(), 50));
    String host = URI.create(context.getJob().getUrl()).getHost();
    if (host.startsWith("www.")) {
      host = host.substring(4);
    }
    context.setHost(host);
    if (whitelistDomains.contains(host)) {
      context.setRealUrl(true);
      context.setAccepted(true);
    } else {
      if (isValidAddress(context, host)) {
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress(host, 443), 1000);
          context.setRealUrl(true);
        } catch (IOException e) {
          log.error("Not reachable url {}", context.getJob().getUrl());
          context.setRealUrl(false);
        }
      } else {
        context.setRealUrl(false);
      }
    }
    context.setPhase(JobPhase.REAL_URL);
    return context;
  }

  private boolean isValidAddress(JobContext context, String host) {
    boolean isValidAddress = true;
    try {
      InetAddress address = InetAddress.getByName(host);
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress()) {
        isValidAddress = false;
      }
    } catch (UnknownHostException e) {
      log.warn("Unknown host exception {}", context.getJob().getUrl());
      isValidAddress = false;
    }
    return isValidAddress;
  }

}
