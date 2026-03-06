package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.EngineType;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface AiModelRepository extends JpaRepository<AiModelEntity, Long> {

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModel.queries")
  })
  List<AiModelEntity> findByProvider(EngineType provider);

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModel.queries")
  })
  Optional<AiModelEntity> findByProviderAndModel(EngineType provider, String model);

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModel.queries")
  })
  @Query("SELECT DISTINCT am FROM AiModelEntity am LEFT JOIN FETCH am.capabilities amc WHERE amc.enabled = true OR amc IS NULL")
  List<AiModelEntity> findAllWithEnabledCapabilities();
}
