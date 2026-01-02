package com.jobshunter.controller;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.EngineConfigurationRepository;
import com.jobshunter.database.service.UserDataService;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.JobOrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EngineController {

  private final EngineConfigurationRepository engineConfigurationRepository;

  private final UserDataService userDataService;

  @GetMapping("/models")
  @Transactional(readOnly = true)
  public ResponseEntity<Map<String, List<String>>> getEngineModels() {
    log.info("Fetching all engine models");
    List<EngineConfigurationEntity> configurations = engineConfigurationRepository.findAll();
    Map<String, List<String>> engineModelsMap = configurations.stream()
        .collect(Collectors.groupingBy(
            config -> config.getEngine().name(),
            Collectors.mapping(
                EngineConfigurationEntity::getModel,
                Collectors.toList()
            )
        ));
    return ResponseEntity.ok(engineModelsMap);
  }

  @PostMapping("/order")
  @Transactional
  public ResponseEntity<?> createOrder(
      @Valid @RequestBody JobOrderRequest request,
      Authentication authentication
  ) {
    if (!request.searchWithUserPrompts() && !request.searchCompanies()) {
      throw new ValidationException("At least one of searchWithUserPrompts or searchCompanies must be true");
    }
    log.info("Creating job order for user: {}, engineId: {}, searchCompanies: {}, searchWithUserPrompts: {}",
        authentication.getName(), request.engineId(), request.searchCompanies(), request.searchWithUserPrompts());

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDataService.getUser(authentication.getName()).get();
    JobOrderEntity jobOrder = userDataService.createJobOrder(user, request.engineId(), request.searchCompanies());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("id", jobOrder.getId(), "message", "Job order created successfully"));
  }

  @GetMapping("/orders")
  @Transactional(readOnly = true)
  public ResponseEntity<List<JobOrderResponse>> getOrders(Authentication authentication) {
    log.info("Fetching job orders for user: {}", authentication.getName());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDataService.getUser(authentication.getName()).get();
    List<JobOrderEntity> orders = userDataService.getUserOrders(user.getId());
    List<JobOrderResponse> responses = orders.stream()
        .map(this::toJobOrderResponse)
        .toList();
    return ResponseEntity.ok(responses);
  }

  private JobOrderResponse toJobOrderResponse(JobOrderEntity order) {
    EngineConfigurationEntity engineConfig = order.getEngineConfiguration();

    return new JobOrderResponse(
        order.getId(),
        engineConfig.getId(),
        engineConfig.getEngine().name(),
        engineConfig.getModel(),
        order.isSearchCompanies(),
        order.getStatus(),
        order.isNotified(),
        order.getTimestamp(),
        order.getErrorMessage()
    );
  }

}
