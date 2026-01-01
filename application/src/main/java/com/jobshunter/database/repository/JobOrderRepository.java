package com.jobshunter.database.repository;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@PackageExpected("com.jobshunter.database.service")
public interface JobOrderRepository extends JpaRepository<JobOrderEntity, Long> {

  List<JobOrderEntity> findByUserId(Long userId);

  @Query("SELECT jo FROM JobOrderEntity jo WHERE jo.user.id = :userId ORDER BY jo.timestamp DESC")
  List<JobOrderEntity> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
}
