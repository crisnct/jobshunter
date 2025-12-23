package com.jobshunter.service.testdata;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.TwilioClient;
import com.twilio.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "false")
public final class DummyTwilioClient implements TwilioClient {

  public static final int TWILLIO_MAX_LIMIT_CHARS = 1600;

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  @Override
  public boolean trySend(String toNumber, String fromNumber, String body) {
    try {
      fromNumber = formatWhatsapp(sanitizePhone(fromNumber));
      toNumber = formatWhatsapp(sanitizePhone(toNumber));
      if (body.length() > TWILLIO_MAX_LIMIT_CHARS) {
        log.error("Message for whatsapp is too long and will be shorten");
        body = body.substring(0, TWILLIO_MAX_LIMIT_CHARS);
      }
      log.info("Sent WhatsApp notification (from={}, to={}, content:\n{}", fromNumber, toNumber, body);
      return true;
    } catch (ApiException ex) {
      log.error("Failed to send WhatsApp message via Twilio from {} to {}: {}", fromNumber, toNumber, ex.getMessage());
      return false;
    }
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

  private String sanitizePhone(String phone) {
    return StringUtils.hasText(phone) ? StringUtils.trimAllWhitespace(phone) : null;
  }

}
