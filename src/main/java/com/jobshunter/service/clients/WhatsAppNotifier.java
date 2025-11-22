package com.jobshunter.service.clients;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.service.application.UserMessagesFactory;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotifier {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private UserMessagesFactory userMessagesFactory;

  private static final DateTimeFormatter JOB_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm");

  @PostConstruct
  private void initializeTwilio() {
    Twilio.init(properties.getWhatsapp().getAccountSid(), properties.getWhatsapp().getAuthToken());
  }

  public void send(List<String> jobsURLs, String username) {
    if (jobsURLs.isEmpty()) {
      log.info("No jobs found. Skipping WhatsApp notification.");
      return;
    }

    String toNumber = formatWhatsapp(resolveUserPhone(username));
    if (!StringUtils.hasText(toNumber)) {
      log.warn("User phone not found. Falling back to configured TWILIO_WHATSAPP_TO (if set).");
      toNumber = formatWhatsapp(properties.getWhatsapp().getToNumber());
    }

    if (!StringUtils.hasText(toNumber)) {
      log.warn("No WhatsApp destination number available. Printing jobs instead.");
      jobsURLs.forEach(job -> log.info("{}", job));
      return;
    }

    String fromNumber = formatWhatsapp(properties.getWhatsapp().getFromNumber());
    String body = this.buildMessage(jobsURLs);

    if (!StringUtils.hasText(fromNumber)) {
      return;
    }
    if (StringUtils.hasText(toNumber) && toNumber.equals(fromNumber)) {
      log.warn("Skipping WhatsApp send because destination equals source ({}).", toNumber);
      return;
    }
    if (!hasCredentials(toNumber, fromNumber)) {
      log.warn("Skipping WhatsApp sender {} -> {} because credentials or number format are invalid.", fromNumber, toNumber);
      return;
    }
    initializeTwilio();
    if (trySend(toNumber, fromNumber, formatJobs(jobsURLs))) {
      return;
    }

    log.warn("WhatsApp send skipped after attempting available senders. Printing jobs to the console instead.");
    jobsURLs.forEach(job -> log.info("{}", job));
  }

  private boolean hasCredentials(String toNumber, String fromNumber) {
    ApplicationProperties.WhatsApp cfg = properties.getWhatsapp();
    return cfg.getAccountSid() != null && !cfg.getAccountSid().isBlank()
        && cfg.getAuthToken() != null && !cfg.getAuthToken().isBlank()
        && StringUtils.hasText(fromNumber)
        && StringUtils.hasText(toNumber)
        && fromNumber.toLowerCase().startsWith("whatsapp:")
        && toNumber.toLowerCase().startsWith("whatsapp:");
  }


  private String resolveUserPhone(String username) {
    if (!StringUtils.hasText(username)) {
      return null;
    }
    return userDataService.getUser(username)
        .map(user -> StringUtils.trimAllWhitespace(user.getPhoneNumber()))
        .filter(StringUtils::hasText)
        .orElse(null);
  }

  private String formatWhatsapp(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String trimmed = StringUtils.trimAllWhitespace(raw);
    // Twilio requires the whatsapp: prefix to send WhatsApp instead of SMS
    if (trimmed.toLowerCase().startsWith("whatsapp:")) {
      return trimmed;
    }
    return "whatsapp:" + trimmed;
  }

  private boolean trySend(String toNumber, String fromNumber, String jobs) {
    try {
      //Message.creator(new com.twilio.type.PhoneNumber(toNumber), new com.twilio.type.PhoneNumber(fromNumber), body).create();

      String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
      Message message = Message.creator(
              new com.twilio.type.PhoneNumber(toNumber),
              new com.twilio.type.PhoneNumber(fromNumber), (String)null)
          .setContentSid("HX6cb8f48ccb191d85060986770ef7e9aa")
          .setContentVariables("{\"timestamp\": \"" + timestamp + "\", \"jobs_links1\": \""+jobs+"\"}")
          .create();

      log.info("Sent WhatsApp notification (from={}, to={})", fromNumber, toNumber);
      return true;
    } catch (ApiException ex) {
      log.error("Failed to send WhatsApp message via Twilio from {} to {}: {}", fromNumber, toNumber, ex.getMessage());
      return false;
    }
  }

  private String buildMessage(List<String> jobs) {
    return userMessagesFactory.build(
        UserMessagesFactory.MessageTemplate.JOBS_NOTIFY,
        Map.of(
            "timestamp", LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT),
            "jobs_links", formatJobs(jobs)
        )
    );
  }

  private String formatJobs(List<String> jobs) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < jobs.size(); i++) {
      if (i > 0) {
        builder.append('\n');
      }
      builder.append(i + 1)
          .append(". ")
          .append(jobs.get(i));
    }
    return builder.toString();
  }
}
