package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.EngineType;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

  List<AiModelEntity> findByProvider(EngineType provider);

  Optional<AiModelEntity> findByProviderAndModel(EngineType provider, String model);

  @Query("SELECT DISTINCT am FROM AiModelEntity am LEFT JOIN FETCH am.capabilities amc WHERE amc.enabled = true OR amc IS NULL")
  List<AiModelEntity> findAllWithEnabledCapabilities();
}
