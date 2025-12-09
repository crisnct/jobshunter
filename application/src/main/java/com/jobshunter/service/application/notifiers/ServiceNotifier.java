package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.Job;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public sealed interface ServiceNotifier permits EmailNotifierService, WhatsappNotifierService {

  DateTimeFormatter JOB_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm", Locale.ENGLISH);

  static String formatJobs(List<Job> jobs) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < jobs.size(); i++) {
      if (i > 0) {
        builder.append('\n');
      }
      Job job = jobs.get(i);
      builder.append(i + 1)
          .append("  ")
          .append(job.score())
          .append("% ")
          .append(" match, ")
          .append(job.url());
    }
    return builder.toString();
  }

  void send(List<Job> jobs, UserEntity user);

}
