package com.jobshunter.database.repository;

import com.jobshunter.database.entities.ModelCapabilityEntity;
import com.jobshunter.model.ModelCapability;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface ModelCapabilityRepository extends JpaRepository<ModelCapabilityEntity, Long> {

  List<ModelCapabilityEntity> findByModelId(Long modelId);

  List<ModelCapabilityEntity> findByCapability(ModelCapability capability);

  void deleteByModelId(Long modelId);
}
