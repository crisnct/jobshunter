package com.jobshunter.service.clients.perplexity;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.dto.perplexityResponse.Annotation;
import com.jobshunter.dto.perplexityResponse.ContentItem;
import com.jobshunter.dto.perplexityResponse.OutputItem;
import com.jobshunter.dto.perplexityResponse.PerplexityResponse;
import com.jobshunter.dto.perplexityResponse.SearchResult;
import com.jobshunter.model.Job;
import com.jobshunter.model.UrlVerificationMode;
import com.jobshunter.processor.PackageExpected;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Anti-hallucination filter. Perplexity warns that models may emit malformed or invented URLs when asked for links inside a JSON answer, so the
 * URLs returned by the model are compared with the URLs the server-side tools really saw ({@code search_results}, {@code fetch_url_results} and
 * message annotations).
 * <p>
 * Fail-open by design: when the response carries no evidence at all (e.g. the provider changed the output shape) nothing is dropped, otherwise a
 * format change would silently turn every search into an empty one.
 */
@Slf4j
@Component
@PackageExpected("com.jobshunter.service.clients.perplexity")
@RequiredArgsConstructor
public class PerplexityUrlVerifier {

  static final String REJECTED_METRIC = "ai.perplexity.url.rejected";

  private final ApplicationProperties properties;
  private final MeterRegistry meterRegistry;

  /**
   * Returns the jobs whose URL is backed by the evidence in {@code response}, according to the configured {@link UrlVerificationMode}.
   */
  public List<Job> verify(List<Job> jobs, PerplexityResponse response) {
    UrlVerificationMode mode = properties.getPerplexity().getUrlVerification();
    if (jobs.isEmpty()) {
      return jobs;
    }
    Evidence evidence = collectEvidence(response);
    if (evidence.isEmpty()) {
      log.warn("Perplexity response {} has no search evidence, skipping URL verification for {} urls", response.id(), jobs.size());
      return jobs;
    }

    Map<Boolean, List<Job>> partitioned = jobs.stream()
        .collect(Collectors.partitioningBy(job -> isBacked(job.getUrl(), evidence, mode)));
    List<Job> rejected = partitioned.get(false);
    if (!rejected.isEmpty()) {
      rejected.forEach(job -> log.info("Perplexity url not found in search results ({}): {}", mode, job.getUrl()));
      meterRegistry.counter(REJECTED_METRIC, "mode", mode.name()).increment(rejected.size());
    }
    return mode == UrlVerificationMode.OFF ? jobs : partitioned.get(true);
  }

  private boolean isBacked(String url, Evidence evidence, UrlVerificationMode mode) {
    if (mode == UrlVerificationMode.HOST) {
      String host = host(url);
      return host != null && evidence.hosts().contains(host);
    }
    String key = key(url);
    return key != null && evidence.urls().contains(key);
  }

  static Evidence collectEvidence(PerplexityResponse response) {
    Set<String> urls = new HashSet<>();
    Set<String> hosts = new HashSet<>();
    if (response.output() != null) {
      for (OutputItem item : response.output()) {
        addAll(urls, hosts, item.results());
        addAll(urls, hosts, item.contents());
        if (item.content() != null) {
          for (ContentItem content : item.content()) {
            if (content.annotations() != null) {
              content.annotations().stream().map(Annotation::url).forEach(url -> add(urls, hosts, url));
            }
          }
        }
      }
    }
    return new Evidence(urls, hosts);
  }

  private static void addAll(Set<String> urls, Set<String> hosts, List<SearchResult> results) {
    if (results != null) {
      results.stream().filter(Objects::nonNull).map(SearchResult::url).forEach(url -> add(urls, hosts, url));
    }
  }

  private static void add(Set<String> urls, Set<String> hosts, String url) {
    String key = key(url);
    String host = host(url);
    if (key != null && host != null) {
      urls.add(key);
      hosts.add(host);
    }
  }

  /** Host without {@code www.}, lower-cased, or null when the url is not an absolute http(s) url. */
  static String host(String url) {
    URI uri = parse(url);
    if (uri == null) {
      return null;
    }
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    return host.startsWith("www.") ? host.substring(4) : host;
  }

  /** Comparison key: host + path + query, ignoring scheme, {@code www.}, fragment and a trailing slash. */
  static String key(String url) {
    URI uri = parse(url);
    if (uri == null) {
      return null;
    }
    String path = uri.getRawPath() == null ? "" : uri.getRawPath();
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
    return host(url) + path + query;
  }

  private static URI parse(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    try {
      URI uri = URI.create(url.trim());
      String scheme = uri.getScheme();
      boolean http = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
      return http && uri.getHost() != null ? uri : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  record Evidence(Set<String> urls, Set<String> hosts) {

    boolean isEmpty() {
      return urls.isEmpty();
    }
  }
}
