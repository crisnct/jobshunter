package com.jobshunter.service.application.notifiers;

import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.service.application.UserMessagesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class EmailNotifierTest {

    private SimpleMailMessage capturedMessage;

    private UserJobRepository userJobRepository;

    private JavaMailSender mailSender;

    private UserMessagesFactory userMessagesFactory;

    private static final DateTimeFormatter JOB_TIMESTAMP_FORMAT
            = DateTimeFormatter.ofPattern("dd-MMMM-yyyy | HH:mm", Locale.ENGLISH);

    @BeforeEach
    void setUp() {
        initUserJobRepo();
        initMailSender();
        initUserMsgFactory();
        setAuthContext();
    }
    @DisplayName("Should send an email with all matching jobs")
    @Test
    void shouldSendEmail() {
        EmailNotifier notifier = new EmailNotifier();
        setField(notifier, "mailSender", mailSender);
        setField(notifier, "userJobRepository", userJobRepository);
        setField(notifier, "userMessagesFactory", userMessagesFactory);

        notifier.send();
        assertNotNull(capturedMessage);
        assertNotNull(capturedMessage.getText());
        assertTrue(capturedMessage.getText().contains("Job1"));
        assertTrue(capturedMessage.getText().contains("Job2"));
        assertFalse(capturedMessage.getText().contains("Job3"));


    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setAuthContext (){
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("unit_user", null)
        );
    }
    private void initUserMsgFactory(){
        userMessagesFactory = new UserMessagesFactory() {
            public String build(MessageTemplate t, Map<String, String> map) {
                return "ts=" + map.get("1") + " jobs=" + List.of("Job1", "Job2");
            }
        };
    }
    private void initMailSender(){
        mailSender = new JavaMailSender() {
            @Override
            public void send(SimpleMailMessage msg) {
                capturedMessage = msg;
            }

            @Override public jakarta.mail.internet.MimeMessage createMimeMessage() { return null; }
            @Override public jakarta.mail.internet.MimeMessage createMimeMessage(java.io.InputStream is) { return null; }
            @Override public void send(jakarta.mail.internet.MimeMessage mimeMessage) {}
            @Override public void send(jakarta.mail.internet.MimeMessage... mimeMessages) {}
            @Override public void send(org.springframework.mail.javamail.MimeMessagePreparator preparator) {}
            @Override public void send(org.springframework.mail.javamail.MimeMessagePreparator... preparators) {}
            @Override public void send(SimpleMailMessage... messages) {}
        };
    }
    private void initUserJobRepo() {
        this.userJobRepository= new UserJobRepository() {
            @Override
            public boolean existsByUserIdAndJobUrl(Long userId, String jobUrl) {
                return false;
            }

            @Override
            public List<String> findJobUrlsByUsernameIgnoreCase(String username) {
                return List.of();
            }

            @Override
            public List<UserJobEntity> findAllByUsernameWithUser(String username) {
                return List.of();
            }

            @Override
            public void flush() {

            }

            @Override
            public <S extends UserJobEntity> S saveAndFlush(S entity) {
                return null;
            }

            @Override
            public <S extends UserJobEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
                return List.of();
            }

            @Override
            public void deleteAllInBatch(Iterable<UserJobEntity> entities) {

            }

            @Override
            public void deleteAllByIdInBatch(Iterable<Long> longs) {

            }

            @Override
            public void deleteAllInBatch() {

            }

            @Override
            public UserJobEntity getOne(Long aLong) {
                return null;
            }

            @Override
            public UserJobEntity getById(Long aLong) {
                return null;
            }

            @Override
            public UserJobEntity getReferenceById(Long aLong) {
                return null;
            }

            @Override
            public <S extends UserJobEntity> List<S> findAll(Example<S> example) {
                return List.of();
            }

            @Override
            public <S extends UserJobEntity> List<S> findAll(Example<S> example, Sort sort) {
                return List.of();
            }

            @Override
            public <S extends UserJobEntity> List<S> saveAll(Iterable<S> entities) {
                return List.of();
            }

            @Override
            public List<UserJobEntity> findAll() {
                return List.of();
            }

            @Override
            public List<UserJobEntity> findAllById(Iterable<Long> longs) {
                return List.of();
            }

            @Override
            public <S extends UserJobEntity> S save(S entity) {
                return null;
            }

            @Override
            public Optional<UserJobEntity> findById(Long aLong) {
                return Optional.empty();
            }

            @Override
            public boolean existsById(Long aLong) {
                return false;
            }

            @Override
            public long count() {
                return 0;
            }

            @Override
            public void deleteById(Long aLong) {

            }

            @Override
            public void delete(UserJobEntity entity) {

            }

            @Override
            public void deleteAllById(Iterable<? extends Long> longs) {

            }

            @Override
            public void deleteAll(Iterable<? extends UserJobEntity> entities) {

            }

            @Override
            public void deleteAll() {

            }

            @Override
            public List<UserJobEntity> findAll(Sort sort) {
                return List.of();
            }

            @Override
            public Page<UserJobEntity> findAll(Pageable pageable) {
                return null;
            }

            @Override
            public <S extends UserJobEntity> Optional<S> findOne(Example<S> example) {
                return Optional.empty();
            }

            @Override
            public <S extends UserJobEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
                return null;
            }

            @Override
            public <S extends UserJobEntity> long count(Example<S> example) {
                return 0;
            }

            @Override
            public <S extends UserJobEntity> boolean exists(Example<S> example) {
                return false;
            }

            @Override
            public <S extends UserJobEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
                return null;
            }
        };

    }

}
