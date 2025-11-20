package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.repository.UserRepository;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotifier {

  private final ApplicationProperties properties;
  private final UserRepository userRepository;
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  public void send(List<String> opportunities, String username) {
    if (opportunities.isEmpty()) {
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
      opportunities.forEach(job -> log.info("{}", job));
      return;
    }

    String fromNumber = formatWhatsapp(properties.getWhatsapp().getFromNumber());
    String body = buildMessage(opportunities);

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
    if (trySend(body, toNumber, fromNumber)) {
      return;
    }

    log.warn("WhatsApp send skipped after attempting available senders. Printing jobs to the console instead.");
    opportunities.forEach(job -> log.info("{}", job));
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

  private void initializeTwilio() {
    if (initialized.compareAndSet(false, true)) {
      Twilio.init(properties.getWhatsapp().getAccountSid(), properties.getWhatsapp().getAuthToken());
    }
  }

  private String resolveUserPhone(String username) {
    if (!StringUtils.hasText(username)) {
      return null;
    }
    return userRepository.findByUsername(username)
        .map(user -> StringUtils.trimWhitespace(user.getPhoneNumber()))
        .filter(StringUtils::hasText)
        .orElse(null);
  }

  private String formatWhatsapp(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String trimmed = StringUtils.trimWhitespace(raw);
    // Twilio requires the whatsapp: prefix to send WhatsApp instead of SMS
    if (trimmed.toLowerCase().startsWith("whatsapp:")) {
      return trimmed;
    }
    return "whatsapp:" + trimmed;
  }

  private boolean trySend(String body, String toNumber, String fromNumber) {
    try {
      Message.creator(
              new com.twilio.type.PhoneNumber(toNumber),
              new com.twilio.type.PhoneNumber(fromNumber),
              body
          )
          .create();
      log.info("Sent WhatsApp notification (from={}, to={})", fromNumber, toNumber);
      return true;
    } catch (ApiException ex) {
      log.error("Failed to send WhatsApp message via Twilio from {} to {}: {}", fromNumber, toNumber, ex.getMessage());
      return false;
    }
  }

  private String buildMessage(List<String> opportunities) {
    StringBuilder builder = new StringBuilder("Jobshunter rezultate zilnice:\n");
    opportunities.forEach(job -> builder
        .append("- ")
        .append(job)
        .append('\n'));
    return builder.toString();
  }
}
