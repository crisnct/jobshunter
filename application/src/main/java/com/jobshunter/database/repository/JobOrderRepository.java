package com.jobshunter.database.repository;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface JobOrderRepository extends JpaRepository<JobOrderEntity, Long> {

  @Query("SELECT jo FROM JobOrderEntity jo WHERE jo.user.id = :userId ORDER BY jo.modifiedAt DESC, jo.status ASC")
  List<JobOrderEntity> findByUserIdOrderByModifiedAtDescAndStatus(@Param("userId") Long userId);

}
