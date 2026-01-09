package com.jobshunter.service.clients.browser;

import java.net.URI;
import java.util.List;
import org.apache.hc.client5.http.protocol.HttpClientContext;

public record HttpFetchResult(
    String originalUrl,
    String finalUrl,
    int statusCode,
    String body,
    List<URI> redirects,
    HttpClientContext context
) {

}
