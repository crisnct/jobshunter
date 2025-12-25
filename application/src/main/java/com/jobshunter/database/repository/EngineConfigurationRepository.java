package com.jobshunter.database.repository;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EngineConfigurationRepository extends JpaRepository<EngineConfigurationEntity, Long> {

  Optional<EngineConfigurationEntity> findByEngineTypeAndTier(EngineType engineType, EngineTier tier);
}

