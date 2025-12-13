package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.Job;
import com.jobshunter.service.application.UserMessagesFactory;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class EmailNotifierIT {


    @Mock
    private SmtpMailtrapClient emailClient;

    @Mock
    private UserMessagesFactory userMessagesFactory;

    @InjectMocks
    private EmailNotifierService emailNotifierService;



    @Test
    public void shouldSendEmailNotification() throws MessagingException, IOException {
        UserEntity user = new UserEntity();

        String expectedBody = "You have new job opportunities!";
        when(userMessagesFactory.build(
                any(UserMessagesFactory.MessageTemplate.class),
                any(Map.class)
        )).thenReturn(expectedBody);

        emailNotifierService.send(List.of(), user);


        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailClient).sendEmail(
                toCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture(),
                eq(null)
        );

        assertEquals(user.getEmail(), toCaptor.getValue());
        assertEquals("JobsHunter - new jobs for you", subjectCaptor.getValue());
        assertEquals(expectedBody, bodyCaptor.getValue());


    }


}

