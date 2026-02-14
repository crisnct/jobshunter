package com.jobshunter.service.clients.browser;

import org.apache.hc.client5.http.protocol.HttpClientContext;

public record HttpFetchResult(
    int statusCode,
    String body,
    HttpClientContext context
) {

}
