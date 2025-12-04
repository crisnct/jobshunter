package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.service.application.UserMessagesFactory;
import com.twilio.rest.bulkexports.v1.export.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class EmailNotifier implements INotifier {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserJobRepository userJobRepository;

    @Autowired
    private UserMessagesFactory userMessagesFactory;

    @Autowired
    UserDataService userDataService;

    private static final DateTimeFormatter JOB_TIMESTAMP_FORMAT
            = DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm", Locale.ENGLISH);

    @Override
    public void send(List<Job> jobs, String username) {
        UserEntity user = userDataService.getUser(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
        String body = userMessagesFactory.build(UserMessagesFactory.MessageTemplate.JOBS_NOTIFY, Map.of("1", timestamp, "k2", jobs.stream().map(Job::getUrl).toString()));
        trySend("", user.getEmail(),body);
    }

    private void trySend (String from, String to, String body){
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setText(body);
            mailSender.send(msg);
        } catch (MailException e) {
           log.error(e.getMessage());
        }
    }
}
