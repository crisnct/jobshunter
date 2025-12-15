package com.jobshunter.service.clients;

import com.jobshunter.processor.PackageExpected;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@PackageExpected("com.jobshunter.service.application.notifiers")
public class SmtpMailtrapClient {

  private static final String DEFAULT_SUBJECT = "JobsHunter notification";

  @Autowired
  private JavaMailSender mailSender;

  @Value("${spring.mail.from:}")
  private String configuredFrom;

  public void sendEmail(
      @NonNull String to,
      @Nullable String subject,
      @NonNull String body,
      @Nullable MultipartFile attachment
  ) {
    try {
      log.info("Sending email with attachement to {} with subject {}", to, subject);
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
      helper.setFrom(configuredFrom);
      helper.setTo(to);
      helper.setSubject(StringUtils.hasText(subject) ? subject : DEFAULT_SUBJECT);
      helper.setText(StringUtils.hasText(body) ? body : "");
      if (attachment != null && !attachment.isEmpty()) {
        helper.addAttachment(
            StringUtils.hasText(attachment.getOriginalFilename()) ? attachment.getOriginalFilename() : attachment.getName(),
            attachment);
      }
      mailSender.send(mimeMessage);
      log.info("Email sent successfully");
    } catch (MessagingException | MailException e) {
      log.error("Failed to send email with attachment to {}: {}", to, e.getMessage());
      throw new IllegalStateException("Failed to send email", e);
    }
  }

}


