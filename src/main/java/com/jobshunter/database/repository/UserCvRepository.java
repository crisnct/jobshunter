package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserCv;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCvRepository extends JpaRepository<UserCv, Long> {

  Optional<UserCv> findByUserIdAndFilename(Long userId, String filename);

  java.util.List<UserCv> findByUserId(Long userId);

  void deleteByUserId(Long userId);

  void deleteByUserIdAndFilename(Long userId, String filename);
}
