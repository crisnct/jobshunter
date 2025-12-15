package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.processor.PackageExpected;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

@PackageExpected("com.jobshunter.database.service")
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByVerificationToken(String verificationToken);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneIgnoreCase(String phonedNumber);
}
