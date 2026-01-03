package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.EngineType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

  List<AiModelEntity> findByProvider(EngineType provider);

  Optional<AiModelEntity> findByProviderAndModel(EngineType provider, String model);
}
