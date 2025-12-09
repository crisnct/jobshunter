package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.dto.Job;
import com.jobshunter.service.application.UserMessagesFactory;
import com.jobshunter.service.application.UserMessagesFactory.MessageTemplate;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public final class EmailNotifierService implements Notifier {

  @Autowired
  private SmtpMailtrapClient emailClient;

  @Autowired
  private UserJobRepository userJobRepository;

  @Autowired
  private UserMessagesFactory userMessagesFactory;

  @Override
  public void send(List<Job> jobs, UserEntity user) {
    String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
    String body = userMessagesFactory.build(MessageTemplate.JOBS_NOTIFY, Map.of("1", timestamp, "2", Notifier.formatJobs(jobs)));
    emailClient.sendEmail(user.getEmail(), "JobsHunter - new jobs for you", body, null);
  }

}


