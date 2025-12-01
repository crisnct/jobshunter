package com.jobshunter.service.notifiers;

import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SimpleEmailNotifier implements INotifier {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserJobRepository userJobRepository;

    @Override
    public void send() {
        /*
        * Get all thw jobs matching the user's set criteria
        * Format each job to a simple email format
        * Send the email
        * */
        String username = UserContext.getCurrentUsername();
        userJobRepository.findAllByUsernameWithUser(username);
        this.mailSender.send(new SimpleMailMessage());
    }
}
