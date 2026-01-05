package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserCvRepository extends JpaRepository<UserCvEntity, Long> {

  Optional<UserCvEntity> findByUserId(Long userId);

  Optional<UserCvEntity> findByUserUsernameIgnoreCase(@SqlInjectionSafe String username);

  void deleteByUserId(Long userId);
}

