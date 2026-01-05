package com.jobshunter.database.repository;

import com.jobshunter.database.entities.JobOrderResultEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface JobOrderResultRepository extends JpaRepository<JobOrderResultEntity, Long> {

  List<JobOrderResultEntity> findByJobOrderId(Long jobOrderId);

  List<JobOrderResultEntity> findByJobId(Long jobId);
}
