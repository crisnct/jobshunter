package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserPromptRepository extends JpaRepository<UserPromptEntity, Long> {

  Optional<UserPromptEntity> findByIdAndUserId(Long id, Long userId);

}

