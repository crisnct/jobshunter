package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.google.ParseResult;
import com.jobshunter.service.clients.google.SerpApiJobsClient;
import io.jsonwebtoken.lang.Collections;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/test")
@PreAuthorize("hasRole('TEST')")
public class TestController {

  @Autowired
  private EmailNotifierService emailNotifierService;

  @Autowired
  private UserDataService userDataService;

  @Autowired
  private SerpApiJobsClient serpApi;

  @PostMapping(value = "/email/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> send(
      @Valid @ModelAttribute EmailRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    log.info("Sending email to {} initiated by {}", request.getEmail(), userDetails.getUsername());
    emailNotifierService.sendCustomEmail(request.getEmail(), request.getSubject(), request.getMessage(), request.getFile());
    log.info("Email sent successfully to {}", request.getEmail());
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/email/sendUsingTemplate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> sendUsingTemplate(
      @Valid @RequestBody JobHuntResponse jobs,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    UserEntity user = userDataService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    log.info("Sending email to {}", user.getEmail());
    emailNotifierService.sendUsingTemplate(jobs.jobsFound(), user);
    log.info("Email sent successfully to {}", user.getEmail());
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/searchWithSerp", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> searchJobsWithSERP(
      @Valid @RequestBody SearchWithSerpRequest request,
      @AuthenticationPrincipal UserDetails userDetails
  ) throws IOException {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    UserEntity user = userDataService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    log.info("Searching jobs for {}", user.getUsername());
    ParseResult jobs = serpApi.searchJobs(request);
    if (Collections.isEmpty(jobs.jobs())) {
      return ResponseEntity.badRequest().body(Map.of("error", jobs.error()));
    } else {
      return ResponseEntity.ok(jobs);
    }
  }

  public record SearchWithSerpRequest(
      String query,
      String engine,
      String country,
      String language
  ) {

  }

}
