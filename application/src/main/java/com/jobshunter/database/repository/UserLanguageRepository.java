package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserLanguageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [Issue #46] Repository for the user-language junction table.
 * Provides queries to find all languages associated with a given user.
 */
@Repository
public interface UserLanguageRepository extends JpaRepository<UserLanguageEntity, Long> {

  List<UserLanguageEntity> findByUser(UserEntity user);
}
