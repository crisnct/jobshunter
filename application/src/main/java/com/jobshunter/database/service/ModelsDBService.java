package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.AiModelsCapabilityEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.AiModelsCapabilityRepository;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.model.EngineType;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ModelsDBService {

  private final AiModelRepository aiModelRepository;
  private final AiModelsCapabilityRepository aiModelsCapabilityRepository;

  public List<AiModelEntity> getAllModels() {
    return aiModelRepository.findAll();
  }

  public Optional<AiModelEntity> getModelById(EngineType provider, String model) {
    return aiModelRepository.findByProviderAndModel(provider, model);
  }

  public List<AiCapabilityType> getCapabilities(AiModelEntity model) {
    return aiModelsCapabilityRepository.findByIdModelIdAndEnabledTrue(model.getId())
        .stream()
        .map(entity -> entity.getCapability().getType())
        .toList();
  }

  public List<AiModelEntity> getAllModelsWithCapabilities() {
    return aiModelRepository.findAllWithEnabledCapabilities();
  }

  @Transactional(readOnly = true)
  public void initialize(AiModelEntity model) {
    Hibernate.initialize(model);
    Hibernate.initialize(model.getCapabilities());
    for (AiModelsCapabilityEntity entity: model.getCapabilities()){
      Hibernate.initialize(entity.getCapability());
    }
  }

}
