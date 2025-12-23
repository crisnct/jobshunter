package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.processor.PackageExpected;
import com.jobshunter.processor.SqlInjectionSafe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@PackageExpected("com.jobshunter.database.service")
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(@SqlInjectionSafe String username);

    Optional<UserEntity> findByVerificationToken(@SqlInjectionSafe String verificationToken);

    boolean existsByUsernameIgnoreCase(@SqlInjectionSafe String username);

    boolean existsByEmailIgnoreCase(@SqlInjectionSafe String email);

    boolean existsByPhoneNumberIgnoreCase(@SqlInjectionSafe String phonedNumber);

    @Query("SELECT u.email FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    List<String> findEmailsByRole(
        @Param("roleName")
        @SqlInjectionSafe
        String roleName
    );

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.prompts WHERE lower(u.username) = lower(:username)")
    Optional<UserEntity> findByUsernameWithPrompts(
        @Param("username")
        @SqlInjectionSafe
        String username
    );

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.prompts")
    List<UserEntity> findAllWithPrompts();
}
