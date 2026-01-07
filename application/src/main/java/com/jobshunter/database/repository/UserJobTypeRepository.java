package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserJobTypeRepository extends JpaRepository<UserJobTypeEntity, Long> {

  List<UserJobTypeEntity> findByUserId(Long userId);

  @Modifying
  @Query("""
      DELETE FROM UserJobTypeEntity ujt 
      WHERE ujt.user.id = :userId
      """)
  void deleteByUserId(@Param("userId") Long userId);
}
