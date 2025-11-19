package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppNotifier {

    private final ApplicationProperties properties;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public void send(List<String> opportunities) {
        if (opportunities.isEmpty()) {
            log.info("No jobs found. Skipping WhatsApp notification.");
            return;
        }
        if (!hasCredentials()) {
            log.warn("Twilio credentials not configured. Printing jobs to the console instead.");
            opportunities.forEach(job -> log.info("{}", job));
            return;
        }

        initializeTwilio();
        String body = buildMessage(opportunities);
        Message.creator(
                        new com.twilio.type.PhoneNumber(properties.getWhatsapp().getToNumber()),
                        new com.twilio.type.PhoneNumber(properties.getWhatsapp().getFromNumber()),
                        body)
                .create();
        log.info("Sent WhatsApp notification with {} jobs", opportunities.size());
    }

    private boolean hasCredentials() {
        ApplicationProperties.WhatsApp cfg = properties.getWhatsapp();
        return cfg.getAccountSid() != null && !cfg.getAccountSid().isBlank()
                && cfg.getAuthToken() != null && !cfg.getAuthToken().isBlank()
                && cfg.getFromNumber() != null && !cfg.getFromNumber().isBlank()
                && cfg.getToNumber() != null && !cfg.getToNumber().isBlank();
    }

    private void initializeTwilio() {
        if (initialized.compareAndSet(false, true)) {
            Twilio.init(properties.getWhatsapp().getAccountSid(), properties.getWhatsapp().getAuthToken());
        }
    }

    private String buildMessage(List<String> opportunities) {
        StringBuilder builder = new StringBuilder("🧠 Jobshunter – rezultate zilnice:\n");
        opportunities.forEach(job -> builder
                .append("• ")
                .append(job)
                .append('\n'));
        return builder.toString();
    }
}
