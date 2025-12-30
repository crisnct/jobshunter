package com.jobshunter.controller;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.repository.EngineConfigurationRepository;
import com.jobshunter.model.EngineType;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ModelsController {

  private final EngineConfigurationRepository engineConfigurationRepository;

  @GetMapping
  public ResponseEntity<List<String>> getModels(
      @RequestParam("engine")
      @NotNull
      EngineType engine
  ) {
    log.info("Fetching models for engine: {}", engine);
    List<EngineConfigurationEntity> configurations = engineConfigurationRepository.findByEngine(engine);
    List<String> models = configurations.stream()
        .map(config -> config.getModel())
        .toList();
    return ResponseEntity.ok(models);
  }

}
