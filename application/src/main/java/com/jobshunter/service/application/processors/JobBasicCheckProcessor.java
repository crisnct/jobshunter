package com.jobshunter.service.application.processors;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.model.JobContext;
import com.jobshunter.model.JobPhase;
import jakarta.annotation.Nonnull;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class JobBasicCheckProcessor implements JobProcessor {

  private final Set<String> blacklistDomains;

  public JobBasicCheckProcessor(ApplicationProperties properties) {
    this.blacklistDomains = Arrays.stream(properties.getJobsHunter().getBlacklist().split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public JobContext processAsync(JobContext context) {
    log.info("Checking reachability for URL: {}", context.getJob().getUrl());
    String host = extractHost(context);
    context.setHost(host);
    if (blacklistDomains.contains(host)) {
      log.info("Host is blacklisted {}", host);
      context.setValidatedSuccessfully(false);
      context.failJob("host is blacklisted");
    } else {
      if (context.getJob().getUrl().length() > 2048) {
        //Exceeds the size of column from db
        context.failJob("URL too long, unrealistic, length=" + context.getJob().getUrl().length());
      } else if (isValidAddress(context, host)) {
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress(host, 443), 1000);
          context.setPhase(JobPhase.BASIC_CHECK);
        } catch (IOException e) {
          log.warn("Not reachable url {}", context.getJob().getUrl());
          context.failJob("Not reachable url");
        }
      } else {
        context.failJob("Not reachable IP address");
      }
    }
    return context;
  }

  @Nonnull
  private String extractHost(JobContext context) {
    String host = URI.create(context.getJob().getUrl()).getHost();
    if (host.startsWith("www.")) {
      host = host.substring(4);
    }
    return host;
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
