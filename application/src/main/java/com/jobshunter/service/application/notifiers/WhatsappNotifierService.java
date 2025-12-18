package com.jobshunter.service.application.notifiers;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.Job;
import com.jobshunter.service.application.UserMessagesFactory;
import com.jobshunter.service.application.UserMessagesFactory.MessageTemplate;
import com.jobshunter.service.clients.tinyurl.TinyUrlClient;
import com.jobshunter.service.clients.twilio.TwilioClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class WhatsappNotifierService implements ServiceNotifier {

  @Autowired
  private ApplicationProperties properties;

  @Autowired
  private UserMessagesFactory userMessagesFactory;

  @Autowired
  private TwilioClient twilioClient;

  @Autowired
  private TinyUrlClient tinyUrlClient;

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

    List<Job> jobsToSend = jobsURLs;
    String formattedJobs = ServiceNotifier.formatJobs(jobsToSend);
    if (formattedJobs.length() > TwilioClient.TWILLIO_MAX_LIMIT_CHARS) {
      jobsToSend = jobsURLs.stream().map(job -> {
        try {
          return new Job(job.score(), tinyUrlClient.shorten(job.url()), job.source());
        } catch (Exception e) {
          log.error("Can not shorten url " + job);
          return job;
        }
      }).toList();
      formattedJobs = ServiceNotifier.formatJobs(jobsToSend);
    }

    String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
    String body = userMessagesFactory.build(MessageTemplate.JOBS_NOTIFY, Map.of("1", timestamp, "2", formattedJobs));

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
