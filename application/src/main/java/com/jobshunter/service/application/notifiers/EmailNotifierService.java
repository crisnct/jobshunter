package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import com.jobshunter.service.application.UserMessagesFactory;
import com.jobshunter.service.application.UserMessagesFactory.MessageTemplate;
import com.jobshunter.service.clients.RestMailtrapClient;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import com.jobshunter.service.clients.tinyurl.TinyUrlClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public final class EmailNotifierService implements ServiceNotifier {

  private final SmtpMailtrapClient emailClient;
  private final RestMailtrapClient restMailtrapClient;
  private final UserMessagesFactory userMessagesFactory;
  private final TinyUrlClient tinyUrlClient;

  public void sendCustomEmail(String to, String subject, String body, MultipartFile attachment) {
    emailClient.sendEmail(List.of(to), subject, body, attachment);
  }

  @Override
  public void send(List<Job> jobs, UserEntity user) {
    String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
    String body = userMessagesFactory.build(MessageTemplate.JOBS_NOTIFY, Map.of("1", timestamp, "2", ServiceNotifier.formatJobs(jobs)));
    emailClient.sendEmail(user.getEmail(), "JobsHunter - new jobs for you", body);
  }

  @Override
  public void sendUsingTemplate(List<Job> jobs, UserEntity user) {
    String body = formatJobs(jobs, false);
    if (body.length() >= 10000) {
      body = formatJobs(jobs, true);
    }
    restMailtrapClient.sendEmailWithNewJobs(user.getUsername(), user.getEmail(), body);
  }

  private String formatJobs(List<Job> jobs, boolean shortenUrls) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < jobs.size(); i++) {
      if (i > 0) {
        builder.append("\n\n");
      }
      Job job = jobs.get(i);
      builder.append(i + 1)
//          .append(".  Match: ")
//          .append(job.getScore())
//          .append("%")
          .append(". Source: ")
          .append(job.getSource())
          .append(", URL: ")
          .append(shortenUrls ? tinyUrlClient.shorten(job.getUrl()) : job.getUrl());
    }
    return builder.toString();
  }

  public void sendVerificationToken(UserEntity user) {
    String body = userMessagesFactory.build(MessageTemplate.TOKEN,
        Map.of("1", user.getUsername(), "2", user.getVerificationToken()));
    emailClient.sendEmail(user.getEmail(), "JobsHunter - verification token", body);
  }

  public void sendMailToApproveAccount(UserEntity user, List<String> emailAddresses) {
    String body = userMessagesFactory.build(MessageTemplate.APPROVE_ACCOUNT,
        Map.of("1", user.getUsername(), "2", user.getEmail()));
    emailClient.sendEmail(emailAddresses, "JobsHunter - approve account", body, null);
  }

  public void accountRejected(UserEntity user, String rejectReason) {
    String body = userMessagesFactory.build(MessageTemplate.ACCOUNT_REJECTED,
        Map.of("1", user.getUsername(), "2", rejectReason));
    emailClient.sendEmail(user.getEmail(), "JobsHunter - account rejected", body);
  }

  public void accountApproved(UserEntity user) {
    String body = userMessagesFactory.build(MessageTemplate.ACCOUNT_APPROVED, Map.of("1", user.getUsername()));
    emailClient.sendEmail(user.getEmail(), "JobsHunter - account approved", body);
  }
}


