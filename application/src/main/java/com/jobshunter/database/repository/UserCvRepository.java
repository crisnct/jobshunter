package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@PackageExpected("com.jobshunter.database.service")
public interface UserCvRepository extends JpaRepository<UserCvEntity, Long> {

  Optional<UserCvEntity> findByUserId(Long userId);

  Optional<UserCvEntity> findByUserUsernameIgnoreCase(String username);

  void deleteByUserId(Long userId);
}

