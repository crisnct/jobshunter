package com.jobshunter.service.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class RetryTemplateTest {

  private static HttpClientErrorException tooManyRequests(String retryAfter) {
    HttpHeaders headers = new HttpHeaders();
    if (retryAfter != null) {
      headers.add("Retry-After", retryAfter);
    }
    return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, new byte[0], StandardCharsets.UTF_8);
  }

  @Test
  void retryAfterSecondsAreConvertedToMillis() {
    assertThat(RetryTemplate.retryAfterMillis(tooManyRequests("7"))).isEqualTo(7_000);
  }

  @Test
  void retryAfterIsCapped() {
    assertThat(RetryTemplate.retryAfterMillis(tooManyRequests("86400"))).isEqualTo(RetryTemplate.MAX_RETRY_AFTER_MILLIS);
  }

  @Test
  void nonNumericOrMissingRetryAfterIsIgnored() {
    assertThat(RetryTemplate.retryAfterMillis(tooManyRequests("Wed, 21 Oct 2026 07:28:00 GMT"))).isZero();
    assertThat(RetryTemplate.retryAfterMillis(tooManyRequests(null))).isZero();
  }

  @Test
  void onlyTooManyRequestsIsHonored() {
    HttpClientErrorException notFound = HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nf", new HttpHeaders(), new byte[0],
        StandardCharsets.UTF_8);
    assertThat(RetryTemplate.retryAfterMillis(notFound)).isZero();
    assertThat(RetryTemplate.retryAfterMillis(new IllegalStateException("x"))).isZero();
    assertThat(RetryTemplate.retryAfterMillis(null)).isZero();
  }
}
