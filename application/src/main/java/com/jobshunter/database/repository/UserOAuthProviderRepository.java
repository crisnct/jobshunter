package com.jobshunter.database.repository;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserOAuthProviderEntity;
import com.jobshunter.processor.PackageExpected;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@PackageExpected("com.jobshunter.database.service")
public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProviderEntity, Long> {

  /**
   * Find an OAuth provider link by provider name and provider's unique user ID.
   */
  Optional<UserOAuthProviderEntity> findByProviderAndProviderId(String provider, String providerId);

  /**
   * Find all OAuth providers linked to a specific user.
   */
  List<UserOAuthProviderEntity> findByUser(UserEntity user);

  /**
   * Check if an OAuth provider link already exists.
   */
  boolean existsByProviderAndProviderId(String provider, String providerId);

  /**
   * Check if a user already has a specific provider linked.
   */
  boolean existsByUserAndProvider(UserEntity user, String provider);
}
