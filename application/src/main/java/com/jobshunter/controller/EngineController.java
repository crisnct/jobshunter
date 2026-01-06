package com.jobshunter.controller;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.ModelsDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.JobOrderResponse;
import com.jobshunter.model.AiModel;
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

  private final ModelsDBService modelsDBService;
  private final UserDBService userDBService;
  private final JobOrderDBService jobOrderDBService;

  @GetMapping("/models")
  @Transactional(readOnly = true)
  public ResponseEntity<Map<String, List<AiModel>>> getEngineModels() {
    log.info("Fetching all engine models");
    List<AiModelEntity> configurations = modelsDBService.getAllModels();
    Map<String, List<AiModel>> engineModelsMap = configurations.stream()
        .filter(AiModelEntity::isEnabled)
        .collect(Collectors.groupingBy(
            config -> config.getProvider().name(),
            Collectors.mapping(this::toModel, Collectors.toList())
        ));
    return ResponseEntity.ok(engineModelsMap);
  }

  private AiModel toModel(AiModelEntity aiModelEntity) {
    return new AiModel(aiModelEntity.getModel(), aiModelEntity.isEnabled(), aiModelEntity.getNotes());
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
    log.info("Creating job order for user: {}, model: {}, searchCompanies: {}, searchWithUserPrompts: {}",
        authentication.getName(), request.model(), request.searchCompanies(), request.searchWithUserPrompts());

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDBService.getUser(authentication.getName()).get();
    JobOrderEntity jobOrder = jobOrderDBService.createJobOrder(user, request);
    log.info("Job order created successfully");
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("id", jobOrder.getId(), "message", "Job order created successfully"));
  }

  @GetMapping("/orders")
  @Transactional(readOnly = true)
  public ResponseEntity<List<JobOrderResponse>> getOrders(Authentication authentication) {
    log.info("Fetching job orders for user: {}", authentication.getName());
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    UserEntity user = userDBService.getUser(authentication.getName()).get();
    List<JobOrderEntity> orders = jobOrderDBService.getUserOrders(user.getId());
    List<JobOrderResponse> responses = orders.stream()
        .map(this::toJobOrderResponse)
        .toList();
    return ResponseEntity.ok(responses);
  }

  private JobOrderResponse toJobOrderResponse(JobOrderEntity order) {
    AiModelEntity aiModel = order.getAiModel();

    return new JobOrderResponse(
        order.getId(),
        aiModel.getId(),
        aiModel.getProvider().name(),
        aiModel.getModel(),
        order.isSearchCompanies(),
        order.isSearchByPrompts(),
        order.getStatus(),
        order.isNotified(),
        order.getTimestamp(),
        order.getErrorMessage()
    );
  }

}
