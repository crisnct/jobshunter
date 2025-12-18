package com.jobshunter.service.clients.mailtrap;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.RestMailtrapClient;
import com.jobshunter.service.clients.mailtrap.RestMailtrapClientImpl.MailtrapTemplateRequest.From;
import com.jobshunter.service.clients.mailtrap.RestMailtrapClientImpl.MailtrapTemplateRequest.To;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "jobshunter.useDummyData", havingValue = "false")
public non-sealed class RestMailtrapClientImpl implements RestMailtrapClient {

  private static final URI MAILTRAP_URI = URI.create("https://send.api.mailtrap.io/api/send");

  @Autowired
  private RestClient restClient;

  @Value("${jobshunter.name:}")
  private String appName;

  @Value("${spring.mail.from:}")
  private String configuredFrom;

  @Value("${spring.mail.apiKey:}")
  private String apiKey;

  @Value("${spring.mail.templateUUID:}")
  private String templateUUID;

  @Override
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
        .header("Authorization", "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .onStatus(HttpStatusCode::isError, (req, res) -> {
          String error = new String(res.getBody().readAllBytes());
          throw new IllegalStateException("Mailtrap failed: " + res.getStatusCode() + " " + error);
        })
        .onStatus(HttpStatusCode::is2xxSuccessful, (req, res) -> {
          log.info("Email send successfully to {}", email);
        })
        .toBodilessEntity();
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
