package com.jobshunter.service.clients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TinyUrlClient {

  @Autowired
  private RestTemplate restTemplate;

  public String shorten(String longUrl) {
    String apiUrl = UriComponentsBuilder
        .fromHttpUrl("https://tinyurl.com/api-create.php")
        .queryParam("url", longUrl)
        .toUriString();

    return restTemplate.getForObject(apiUrl, String.class);
  }

}
