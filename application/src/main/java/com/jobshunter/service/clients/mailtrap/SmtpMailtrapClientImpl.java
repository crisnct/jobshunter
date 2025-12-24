package com.jobshunter.service.clients.mailtrap;

import com.jobshunter.processor.PackageExpected;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true")
public non-sealed class SmtpMailtrapClientImpl implements SmtpMailtrapClient {

  private static final String DEFAULT_SUBJECT = "JobsHunter notification";

  private final JavaMailSender mailSender;

  @Value("${spring.mail.from:}")
  private String configuredFrom;

  public SmtpMailtrapClientImpl(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void sendEmail(
      @NonNull String to,
      @Nullable String subject,
      @NonNull String body
  ) {
    sendEmail(List.of(to), subject, body, null);
  }

  @Override
  public void sendEmail(
      @NonNull List<String> to,
      @Nullable String subject,
      @NonNull String body,
      @Nullable MultipartFile attachment
  ) {
    try {
      log.info("Sending email with attachement to {} with subject {}", to, subject);
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
      helper.setFrom(configuredFrom);
      helper.setTo(to.toArray(String[]::new));
      helper.setSubject(StringUtils.hasText(subject) ? subject : DEFAULT_SUBJECT);
      helper.setText(StringUtils.hasText(body) ? body : "");
      if (attachment != null && !attachment.isEmpty()) {
        helper.addAttachment(
            StringUtils.hasText(attachment.getOriginalFilename()) ? attachment.getOriginalFilename() : attachment.getName(),
            attachment);
      }
      mailSender.send(mimeMessage);
      log.info("Email sent successfully to {}", (Object) to.toArray(String[]::new));
    } catch (MessagingException | MailException e) {
      log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
      throw new IllegalStateException("Failed to send email", e);
    }
  }

}


