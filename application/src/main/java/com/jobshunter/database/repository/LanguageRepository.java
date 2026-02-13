package com.jobshunter.database.repository;

import com.jobshunter.database.entities.LanguageEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [Issue #46] Repository for the languages reference table.
 * Provides lookup by language name for the user-language association flow.
 */
@Repository
public interface LanguageRepository extends JpaRepository<LanguageEntity, Long> {

  Optional<LanguageEntity> findByName(String name);
}
