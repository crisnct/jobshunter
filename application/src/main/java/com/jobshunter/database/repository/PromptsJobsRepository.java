package com.jobshunter.database.repository;

import com.jobshunter.database.entities.PromptsJobsEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface PromptsJobsRepository extends JpaRepository<PromptsJobsEntity, PromptsJobsEntity.PromptsJobsId> {

  @Query("""
      SELECT pj FROM PromptsJobsEntity pj 
      WHERE pj.prompt.id = :promptId
      """)
  List<PromptsJobsEntity> findByPromptId(@Param("promptId") Long promptId);

  @Query("""
      SELECT pj FROM PromptsJobsEntity pj 
      WHERE pj.userJob.id = :userJobId
      """)
  List<PromptsJobsEntity> findByUserJobId(@Param("userJobId") Long userJobId);

  @Query("""
      SELECT COUNT(pj) > 0 FROM PromptsJobsEntity pj 
      WHERE pj.prompt.id = :promptId AND pj.userJob.id = :userJobId
      """)
  boolean existsByPromptIdAndUserJobId(@Param("promptId") Long promptId, @Param("userJobId") Long userJobId);

  @Modifying
  @Transactional
  @Query("""
      DELETE FROM PromptsJobsEntity pj 
      WHERE pj.prompt.id = :promptId
      """)
  void deleteByPromptId(@Param("promptId") Long promptId);

  @Modifying
  @Transactional
  @Query("""
      DELETE FROM PromptsJobsEntity pj 
      WHERE pj.userJob.id = :userJobId
      """)
  void deleteByUserJobId(@Param("userJobId") Long userJobId);
}
