package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  @QueryHints({
          @QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = HibernateHints.HINT_CACHE_REGION, value = "user.queries")
  })
  Optional<UserEntity> findByUsername(@SqlInjectionSafe String username);

  @QueryHints({
          @QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"),
          @QueryHint(name = HibernateHints.HINT_CACHE_REGION, value = "user.queries")
  })
  Optional<UserEntity> findByEmail(@SqlInjectionSafe String email);

  Optional<UserEntity> findByVerificationToken(@SqlInjectionSafe String verificationToken);

  boolean existsByUsernameIgnoreCase(@SqlInjectionSafe String username);

  boolean existsByEmailIgnoreCase(@SqlInjectionSafe String email);

  boolean existsByPhoneNumberIgnoreCase(@SqlInjectionSafe String phonedNumber);

  @Query("""
      SELECT u.email FROM UserEntity u 
      JOIN u.roles r 
      WHERE r.name = :roleName
      """)
  List<String> findEmailsByRole(
      @Param("roleName")
      @SqlInjectionSafe
      String roleName
  );

}
