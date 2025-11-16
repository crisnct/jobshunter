package com.jobshunter.service;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.model.JobOpportunity;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class WhatsAppNotifier {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotifier.class);
    private final ApplicationProperties properties;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public WhatsAppNotifier(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void send(List<JobOpportunity> opportunities) {
        if (opportunities.isEmpty()) {
            log.info("No jobs found. Skipping WhatsApp notification.");
            return;
        }
        if (!hasCredentials()) {
            log.warn("Twilio credentials not configured. Printing jobs to the console instead.");
            opportunities.forEach(job -> log.info("{} at {} - {}", job.title(), job.company(), job.url()));
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

    private String buildMessage(List<JobOpportunity> opportunities) {
        StringBuilder builder = new StringBuilder("🧠 Jobshunter – rezultate zilnice:\n");
        opportunities.forEach(job -> builder
                .append("• ")
                .append(job.title())
                .append(" @ ")
                .append(job.company())
                .append(" -> ")
                .append(job.url())
                .append('\n'));
        return builder.toString();
    }
}
