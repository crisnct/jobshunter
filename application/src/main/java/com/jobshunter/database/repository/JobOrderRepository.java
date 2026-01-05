package com.jobshunter.database.repository;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface JobOrderRepository extends JpaRepository<JobOrderEntity, Long> {

  @Query("SELECT jo FROM JobOrderEntity jo WHERE jo.user.id = :userId ORDER BY jo.timestamp DESC, jo.status ASC")
  List<JobOrderEntity> findByUserIdOrderByTimestampDescAndStatus(@Param("userId") Long userId);

  @Query("SELECT jo FROM JobOrderEntity jo WHERE jo.status = :status ORDER BY jo.timestamp ASC LIMIT 1")
  Optional<JobOrderEntity> findOldestByStatus(@Param("status") OrderStatus status);

}
