package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.service.application.UserMessagesFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserJobRepository userJobRepository;

    @Mock
    private UserMessagesFactory userMessagesFactory;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> mailMessageCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> placeholdersCaptor;

    private EmailNotifierService notifier;

    @BeforeEach
    void setUp() {
        notifier = new EmailNotifierService();
        ReflectionTestUtils.setField(notifier, "mailSender", mailSender);
        ReflectionTestUtils.setField(notifier, "userJobRepository", userJobRepository);
        ReflectionTestUtils.setField(notifier, "userMessagesFactory", userMessagesFactory);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("unit_user", null));
//        initUserJobRepo();
//        initMailSender();
//        initUserMsgFactory();
//        setAuthContext();

    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("Should send an email built from repository jobs for the authenticated user")
    @Test
    void shouldSendEmailWithFormattedJobs() {
        List<UserJobEntity> jobs = List.of(job("Job1"), job("Job2"));
        when(userJobRepository.findAllByUsernameWithUser("unit_user")).thenReturn(jobs);
        when(userMessagesFactory.build(eq(UserMessagesFactory.MessageTemplate.JOBS_NOTIFY), anyMap()))
                .thenReturn("formatted-body");

        //notifier.send();

        verify(userJobRepository).findAllByUsernameWithUser("unit_user");
        verify(userMessagesFactory).build(eq(UserMessagesFactory.MessageTemplate.JOBS_NOTIFY),
                placeholdersCaptor.capture());
        Map<String, String> placeholders = placeholdersCaptor.getValue();
        assertNotNull(placeholders.get("1"));
        assertEquals(jobs.toString(), placeholders.get("2"));
        assertDoesNotThrow(() -> LocalDateTime.parse(placeholders.get("1"),
                DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm", Locale.ENGLISH)));

        verify(mailSender).send(mailMessageCaptor.capture());
        assertEquals("formatted-body", mailMessageCaptor.getValue().getText());
    }

    private static UserJobEntity job(String jobDescription) {
        return new UserJobEntity() {
            @Override
            public String toString() {
                return jobDescription;
            }
        };
    }
}
