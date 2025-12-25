package com.jobshunter.controller;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.EmailRequest;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.serpRequest.SearchWithSerpRequest;
import com.jobshunter.model.Job;
import com.jobshunter.service.application.JobsValidator;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.AiJobsClient;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
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
  private JobsValidator jobValidator;

  @Autowired
  @Qualifier("EconomyJobsClientSerp")
  private AiJobsClient<SearchWithSerpRequest, List<Job>> serpApi;

  @PostMapping(value = "/email/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> send(
      @Valid
      @ModelAttribute
      EmailRequest request,

      @AuthenticationPrincipal
      UserDetails userDetails
  ) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    emailNotifierService.sendCustomEmail(request.getEmail(), request.getSubject(), request.getMessage(), request.getFile());
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/email/sendUsingTemplate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> sendUsingTemplate(
      @Valid
      @RequestBody
      JobHuntResponse jobs,

      @AuthenticationPrincipal
      UserDetails userDetails
  ) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    UserEntity user = userDataService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    emailNotifierService.sendUsingTemplate(jobs.jobsFound(), user);
    return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
  }

  @PostMapping(value = "/searchWithSerp", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> searchJobsWithSERP(
      @Valid
      @RequestBody
      SearchWithSerpRequest request,

      @AuthenticationPrincipal
      UserDetails userDetails
  ) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    UserEntity user = userDataService.getUser(userDetails.getUsername())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    log.info("Searching jobs for {}", user.getUsername());
    List<Job> jobs = serpApi.searchJobs(request);
    return ResponseEntity.ok(jobs);
  }

  @GetMapping(value = "/redirection")
  public void testRedirection() {
    jobValidator.validateJobs(List.of(new Job(-1,
        "https://vertexaisearch.cloud.google.com/grounding-api-redirect/AUZIYQGjbjtvw3uNWIamk-Oa7putwLsxAOk49Fk9NctPvXtzsw5ubVitrV5BsNerHcy_Z8FX7k_L99wjefDIy3ZUqEZ09Bh_YLXLbaxuY-BRQ41fg0vHzoDC3BSfBoUrdanAtm2OyivQ6NoHzFzlcKRXukz6Yhbt2iyjAwz_TJbGNq5m1Nmg1mu6xJey",
        "Google")));
  }

}
