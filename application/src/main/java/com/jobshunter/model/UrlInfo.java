package com.jobshunter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UrlInfo {

  private final String originalUrl;

  private String url;

  private String rawBody;

  public UrlInfo(String url) {
    this.originalUrl = url;
    this.url = url;
  }


}
