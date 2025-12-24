package com.jobshunter.service.clients.tinyurl;

import com.jobshunter.processor.PackageExpected;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

  public String shorten(String longUrl) {
    String apiUrl = UriComponentsBuilder
        .fromUri(URI.create("https://tinyurl.com/api-create.php"))
        .queryParam("url", longUrl)
        .toUriString();

    return restTemplate.getForObject(apiUrl, String.class);
  }

}
