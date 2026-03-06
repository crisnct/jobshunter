package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiModelsCapabilityEntity;
import com.jobshunter.database.entities.AiModelsCapabilityId;
import com.jobshunter.processor.PackageExpected;
import java.util.List;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface AiModelsCapabilityRepository extends JpaRepository<AiModelsCapabilityEntity, AiModelsCapabilityId> {

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModelCapability.queries")
  })
  List<AiModelsCapabilityEntity> findByIdModelId(Long modelId);

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModelCapability.queries")
  })
  List<AiModelsCapabilityEntity> findByIdCapabilityId(Long capabilityId);

  void deleteByIdModelId(Long modelId);

  void deleteByIdCapabilityId(Long capabilityId);

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModelCapability.queries")
  })
  @Query("SELECT amc FROM AiModelsCapabilityEntity amc JOIN FETCH amc.capability WHERE amc.id.modelId = :modelId AND amc.enabled = true")
  List<AiModelsCapabilityEntity> findByIdModelIdAndEnabledTrue(@Param("modelId") Long modelId);

  @QueryHints({
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHE_REGION, value = "aiModelCapability.queries")
  })
  @Query("SELECT amc FROM AiModelsCapabilityEntity amc JOIN FETCH amc.capability WHERE amc.id.modelId IN :modelIds AND amc.enabled = true")
  List<AiModelsCapabilityEntity> findByIdModelIdInAndEnabledTrueWithCapability(@Param("modelIds") List<Long> modelIds);

}
