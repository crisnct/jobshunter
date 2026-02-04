package com.jobshunter.service.application.notifiers;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.model.UserMessageType;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.clients.TwilioClient;
import com.jobshunter.service.clients.tinyurl.TinyUrlClient;
import com.jobshunter.service.clients.twilio.TwilioClientImpl;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public final class WhatsappNotifierService implements ServiceNotifier {

  private final ApplicationProperties properties;
  private final TemplateRenderer templateRenderer;
  private final TwilioClient twilioClient;
  private final TinyUrlClient tinyUrlClient;

  @Override
  public void send(List<Job> jobsURLs, UserEntity user) {
    if (jobsURLs.isEmpty()) {
      log.info("No jobs found. Skipping WhatsApp notification.");
      return;
    }
    if (user == null) {
      log.warn("User not provided. Skipping WhatsApp notification.");
      return;
    }
    if (!user.isNotifyWhatsapp()) {
      log.info("Skipping WhatsApp notification for {} because notify_whatsapp is disabled.", user.getUsername());
      return;
    }

    String formattedJobs = ServiceNotifier.formatJobs(jobsURLs);
    if (formattedJobs.length() > TwilioClientImpl.TWILLIO_MAX_LIMIT_CHARS) {
      jobsURLs.forEach(job -> {
        try {
          job.setUrl(tinyUrlClient.shorten(job.getUrl()));
        } catch (Exception e) {
          log.error("Can not shorten url " + job);
        }
      });
      formattedJobs = ServiceNotifier.formatJobs(jobsURLs);
    }

    String timestamp = Instant.now().atZone(ZoneId.of("UTC")).format(JOB_TIMESTAMP_FORMAT);
    String body = templateRenderer.getUserMessage(UserMessageType.JOBS_NOTIFY, Map.of("1", timestamp, "2", formattedJobs));

    if (!twilioClient.trySend(user.getPhoneNumber(), properties.getTwilio().getFromNumber(), body)) {
      log.warn("WhatsApp send skipped after attempting available senders. Printing jobs to the console instead.");
      jobsURLs.forEach(job -> log.info("{}", job));
    }
  }

  @Override
  public void sendUsingTemplate(List<Job> jobs, UserEntity user) {
    log.error("Method sendUsingTemplate not implemented!");
  }

}
