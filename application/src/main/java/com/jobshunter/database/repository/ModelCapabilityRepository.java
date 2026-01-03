package com.jobshunter.database.repository;

import com.jobshunter.database.entities.ModelCapabilityEntity;
import com.jobshunter.model.ModelCapability;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelCapabilityRepository extends JpaRepository<ModelCapabilityEntity, Long> {

  List<ModelCapabilityEntity> findByModelId(Long modelId);

  List<ModelCapabilityEntity> findByCapability(ModelCapability capability);

  void deleteByModelId(Long modelId);
}
