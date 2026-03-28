package com.jobshunter.database.repository;

import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

  @QueryHints({
          @QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = HibernateHints.HINT_CACHE_REGION, value = "role.queries")
  })
  Optional<RoleEntity> findByName(@SqlInjectionSafe String name);
}
