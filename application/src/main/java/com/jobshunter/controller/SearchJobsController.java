package com.jobshunter.controller;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.SearchJobResult;
import com.jobshunter.dto.SearchJobsResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.service.application.JobOrderProcessor;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SearchJobsController {

  private final UserDBService userDBService;
  private final JobOrderDBService jobOrderDBService;
  private final JobOrderProcessor jobOrderProcessor;

  @PostMapping("/search_jobs")
  public ResponseEntity<SearchJobsResponse> searchJobs(
      @Valid @RequestBody List<JobOrderRequest> requests,
      Authentication authentication
  ) {
    for (JobOrderRequest request : requests) {
      if (!request.searchWithUserPrompts() && !request.searchCompanies()) {
        throw new ValidationException("At least one of searchWithUserPrompts or searchCompanies must be true");
      }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDBService.getUserCompleteInfo(authentication.getName()).get();

    if (user.getCv() == null) {
      throw new ValidationException("User does not have any cv attached in his profile");
    }

    List<Job> jobsFound = new ArrayList<>();
    for (JobOrderRequest request : requests) {
      log.info("Searching jobs for user: {}, model: {}, searchCompanies: {}, searchWithUserPrompts: {}",
          authentication.getName(), request.model(), request.searchCompanies(), request.searchWithUserPrompts());
      JobOrderEntity jobOrder = jobOrderDBService.createJobOrder(user, request, OrderStatus.PROCESSING);
      JobHuntResponse huntResponse = jobOrderProcessor.process(jobOrder.getId());
      jobsFound.addAll(huntResponse.jobsFound());
      log.info("Job search completed {}-{} with {} jobs", request.model(), request.provider().name(), huntResponse.jobsFound().size());
    }

    List<SearchJobResult> uniqueJobs = jobsFound.stream()
        .distinct()
        .sorted(Comparator.comparing(Job::getScore).reversed())
        .map(job -> new SearchJobResult(job.getUrl(), job.getSource()))
        .toList();
    return ResponseEntity.ok(new SearchJobsResponse(uniqueJobs));
  }

}
