package com.jobshunter.service.clients.twilio;

import com.jobshunter.ApplicationProperties;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.TwilioClient;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jakarta.annotation.PostConstruct;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
public non-sealed class TwilioClientImpl implements TwilioClient {

  public static final int TWILLIO_MAX_LIMIT_CHARS = 1600;

  private final ApplicationProperties properties;

  @PostConstruct
  private void initializeTwilio() {
    var whatsapp = properties.getTwilio();
    Twilio.init(whatsapp.getAccountSid(), whatsapp.getAuthToken());
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  @Override
  @RateLimiter(name = "twilioLimiter")
  @CircuitBreaker(name = "twilio", fallbackMethod = "fallbackSend")
  @Bulkhead(name = "twilioBulkhead")
  public boolean trySend(String toNumber, String fromNumber, String body) {
    try {
      fromNumber = formatWhatsapp(sanitizePhone(fromNumber));
      toNumber = formatWhatsapp(sanitizePhone(toNumber));
      if (body.length() > TWILLIO_MAX_LIMIT_CHARS) {
        log.error("Message for whatsapp is too long and will be shorten");
        body = body.substring(0, TWILLIO_MAX_LIMIT_CHARS);
      }
      Message message = Message
          .creator(new com.twilio.type.PhoneNumber(toNumber), new com.twilio.type.PhoneNumber(fromNumber), body)
          .create();
      log.info("Sent WhatsApp notification (from={}, to={}, SID={})", fromNumber, toNumber, message.getSid());
      return true;
    } catch (ApiException ex) {
      log.error("Failed to send WhatsApp message via Twilio from {} to {}: {}", fromNumber, toNumber, ex.getMessage());
      return false;
    }
  }

  @SuppressWarnings("unused")
  private boolean fallbackSend(String toNumber, String fromNumber, String body, Throwable throwable) {
    log.error("Twilio send failed (circuit open/fallback): {}", throwable.getMessage());
    return false;
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
