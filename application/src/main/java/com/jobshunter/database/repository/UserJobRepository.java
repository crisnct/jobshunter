package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserJobRepository extends JpaRepository<UserJobEntity, Long> {

  Optional<UserJobEntity> findByUserIdAndUrl(Long userId, @SqlInjectionSafe String url);

  @Query("""
      SELECT uj.url FROM UserJobEntity uj 
      WHERE LOWER(uj.user.username) = LOWER(:username)
      """)
  List<String> findJobUrlsByUsernameIgnoreCase(
      @Param("username")
      @SqlInjectionSafe
      String username
  );

  @Query("""
      SELECT uj FROM UserJobEntity uj 
      JOIN FETCH uj.user u 
      WHERE LOWER(u.username) = LOWER(:username)
      """)
  List<UserJobEntity> findAllByUsernameWithUser(
      @Param("username")
      @SqlInjectionSafe
      String username
  );

  @Query("""
      SELECT uj FROM UserJobEntity uj
      JOIN FETCH uj.user u
      WHERE LOWER(u.username) = LOWER(:username)
      AND (:orderId IS NULL OR uj.jobOrder.id = :orderId)
      """)
  List<UserJobEntity> findAllByUsernameWithUserAndOrderId(
      @Param("username")
      @SqlInjectionSafe
      String username,
      @Param("orderId")
      Long orderId
  );

  long countByUserIdAndJobOrderId(Long userId, Long jobOrderId);

  @Query(value = """
      SELECT uj.job_order_id, COUNT(uj.id) 
      FROM user_jobs uj 
      WHERE uj.user_id = :userId 
      AND uj.job_order_id IN :orderIds
      GROUP BY uj.job_order_id
      """, nativeQuery = true)
  List<Object[]> countJobsByOrderIds(@Param("userId") Long userId, @Param("orderIds") List<Long> orderIds);

}
