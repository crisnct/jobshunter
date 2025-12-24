package com.jobshunter.service.clients.tinyurl;

import com.jobshunter.processor.PackageExpected;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
public class TinyUrlClient {

  private final RestTemplate restTemplate;

  public TinyUrlClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @CircuitBreaker(name = "tinyurl", fallbackMethod = "fallbackShorten")
  public String shorten(String longUrl) {
    String apiUrl = UriComponentsBuilder
        .fromUri(URI.create("https://tinyurl.com/api-create.php"))
        .queryParam("url", longUrl)
        .toUriString();

    return restTemplate.getForObject(apiUrl, String.class);
  }

  @SuppressWarnings("unused")
  private String fallbackShorten(String longUrl, Throwable throwable) {
    return longUrl;
  }

}
