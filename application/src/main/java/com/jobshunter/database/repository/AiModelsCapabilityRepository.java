package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiModelsCapabilityEntity;
import com.jobshunter.database.entities.AiModelsCapabilityId;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface AiModelsCapabilityRepository extends JpaRepository<AiModelsCapabilityEntity, AiModelsCapabilityId> {

  List<AiModelsCapabilityEntity> findByIdModelId(Long modelId);

  List<AiModelsCapabilityEntity> findByIdCapabilityId(Long capabilityId);

  void deleteByIdModelId(Long modelId);

  void deleteByIdCapabilityId(Long capabilityId);

}
