package com.jobshunter.controller;

import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.service.application.notifiers.EmailService;
import com.jobshunter.service.clients.SmtpEmailClient;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

  @Autowired
  private EmailService emailService;

  @Autowired
  private SmtpEmailClient emailClient;

  @Autowired
  private UserDataService userDataService;

  @PostMapping(value = "/email/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> send2(
      @Valid @ModelAttribute EmailRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    log.info("Sending email to {} initiated by {}", request.getEmail(), userDetails.getUsername());
    emailClient.sendEmail(request.getEmail(), request.getSubject(), request.getMessage(), request.getFile());
    log.info("Email sent successfully to {}", request.getEmail());
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

}
