package com.jobshunter.database.repository;

import com.jobshunter.database.entities.AiCapabilityEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface AiCapabilityRepository extends JpaRepository<AiCapabilityEntity, Long> {

  Optional<AiCapabilityEntity> findByType(String type);

}
