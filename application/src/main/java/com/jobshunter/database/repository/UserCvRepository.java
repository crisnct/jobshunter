package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserCvRepository extends JpaRepository<UserCvEntity, Long> {

  Optional<UserCvEntity> findByUserId(Long userId);

  @Query("select cv from UserCvEntity cv where lower(cv.user.username) = lower(:username)")
  Optional<UserCvEntity> findByUsernameIgnoreCase(@Param("username") @SqlInjectionSafe String username);

  void deleteByUserId(Long userId);
}

