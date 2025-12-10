package com.jobshunter.database.repository;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@PackageExpected("com.jobshunter.database.service")
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);
}
