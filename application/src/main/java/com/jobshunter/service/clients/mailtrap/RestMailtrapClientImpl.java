package com.jobshunter.service.clients.mailtrap;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.security.JHHeaders;
import com.jobshunter.service.clients.RestMailtrapClient;
import com.jobshunter.service.clients.mailtrap.RestMailtrapClientImpl.MailtrapTemplateRequest.From;
import com.jobshunter.service.clients.mailtrap.RestMailtrapClientImpl.MailtrapTemplateRequest.To;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true")
@RequiredArgsConstructor
public non-sealed class RestMailtrapClientImpl implements RestMailtrapClient {

  private static final URI MAILTRAP_URI = URI.create("https://send.api.mailtrap.io/api/send");

  private final RestClient restClient;

  @Value("${jobshunter.name:}")
  private String appName;

  @Value("${spring.mail.from:}")
  private String configuredFrom;

  @Value("${spring.mail.apiKey:}")
  private String apiKey;

  @Value("${spring.mail.templateUUID:}")
  private String templateUUID;

  @Override
  @RateLimiter(name = "mailtrapLimiter")
  @CircuitBreaker(name = "mailtrap", fallbackMethod = "fallbackSendEmail")
  @Bulkhead(name = "mailtrapBulkhead")
  public void sendEmailWithNewJobs(
      @NonNull String username,
      @NonNull String email,
      @NonNull String body
  ) {
    log.info("Sending email to {}", username);
    MailtrapTemplateRequest payload
        = new MailtrapTemplateRequest(
        new From(configuredFrom, appName),
        List.of(new To(email)),
        templateUUID,
        Map.of("1", username, "2", body)
    );

    restClient.post()
        .uri(MAILTRAP_URI)
        .header(JHHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          String error = new String(res.getBody().readAllBytes());
          throw new RuntimeException("Mailtrap failed: " + res.getStatusCode() + " " + error);
        })
        .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
          log.info("Email send successfully to {}", email);
        })
        .toBodilessEntity();
  }

  @SuppressWarnings("unused")
  private void fallbackSendEmail(String username, String email, String body, Throwable throwable) {
    log.error("Mailtrap REST send failed for {}: {}", email, throwable.getMessage());
  }

  public record MailtrapTemplateRequest(
      From from,
      List<To> to,
      String template_uuid,
      Map<String, Object> template_variables
  ) {

    public record From(String email, String name) {

    }

    public record To(String email) {

    }
  }
}
