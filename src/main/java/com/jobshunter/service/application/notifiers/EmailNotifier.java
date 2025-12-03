package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.service.application.UserMessagesFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class EmailNotifier implements INotifier {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserJobRepository userJobRepository;

    @Autowired
    private UserMessagesFactory userMessagesFactory;

    private static final DateTimeFormatter JOB_TIMESTAMP_FORMAT
            = DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm", Locale.ENGLISH);

    @Override
    public void send() {
        /*
        * Get all thw jobs matching the user's set criteria
        * Format each job to a simple email format
        * Send the email
        * */

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String timestamp = LocalDateTime.now().format(JOB_TIMESTAMP_FORMAT);
        String jobs = userJobRepository.findAllByUsernameWithUser(auth.getName()).toString();
        String body = userMessagesFactory.build(UserMessagesFactory.MessageTemplate.JOBS_NOTIFY, Map.of("1", timestamp, "2", jobs));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setText(body);
        this.mailSender.send(message);
    }

}
