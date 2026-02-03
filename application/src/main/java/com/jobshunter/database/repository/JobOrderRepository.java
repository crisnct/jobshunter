package com.jobshunter.database.repository;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface JobOrderRepository extends JpaRepository<JobOrderEntity, Long> {

  @Query("""
      SELECT jo FROM JobOrderEntity jo 
      WHERE jo.user.id = :userId 
      ORDER BY jo.modifiedAt DESC, jo.status ASC
      """)
  List<JobOrderEntity> findByUserIdOrderByModifiedAtDescAndStatus(@Param("userId") Long userId);

  @Modifying
  @Query(value = "UPDATE job_order SET cost = COALESCE(cost, 0) + :delta WHERE id = :orderId", nativeQuery = true)
  int incrementCost(@Param("orderId") Long orderId, @Param("delta") double delta);

  @Query(value = """
      SELECT jo.user_id, u.username, COALESCE(SUM(jo.cost), 0)
      FROM job_order jo
      JOIN users u ON jo.user_id = u.id
      GROUP BY jo.user_id, u.username
      """, nativeQuery = true)
  List<Object[]> findTotalCostByUser();

  @Query("""
      SELECT jo FROM JobOrderEntity jo 
      WHERE jo.user.id = :userId 
      AND jo.status = 'COMPLETED'
      AND jo.notified = false
      """)
  List<JobOrderEntity> findCompletedAndNotNotifiedByUserId(@Param("userId") Long userId);

}
