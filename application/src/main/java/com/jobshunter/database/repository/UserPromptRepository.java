package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserPromptEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPromptRepository extends JpaRepository<UserPromptEntity, Long> {

  Optional<UserPromptEntity> findByIdAndUserIdAndEngineConfigurationId( Long id, Long userId, Long engineId);

}

