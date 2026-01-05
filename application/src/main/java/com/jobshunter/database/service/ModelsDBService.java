package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.repository.AiModelRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ModelsDBService {

  private final AiModelRepository aiModelRepository;

  public List<AiModelEntity> getAllModels() {
    return aiModelRepository.findAll();
  }

}
