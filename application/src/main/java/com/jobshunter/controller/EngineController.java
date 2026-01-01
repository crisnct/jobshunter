package com.jobshunter.controller;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.repository.EngineConfigurationRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/engine")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EngineController {

  private final EngineConfigurationRepository engineConfigurationRepository;

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

}
