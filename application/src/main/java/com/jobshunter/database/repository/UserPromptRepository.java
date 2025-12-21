package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.EngineType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPromptEntity, Long> {

  List<UserPromptEntity> findAllByUserUsernameIgnoreCase(String username);

  Optional<UserPromptEntity> findByUserIdAndEngine(Long userId, EngineType engine);

}

