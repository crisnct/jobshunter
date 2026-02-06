package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing an OAuth provider linked to a user account.
 * Allows users to have multiple OAuth providers (Google, GitHub, etc.) linked to their account.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_oauth_providers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_oauth_provider_id",
        columnNames = {"provider", "provider_id"}
    )
)
public class UserOAuthProviderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "provider", nullable = false, length = 50)
  private String provider;  // "google", "github", etc.

  @Column(name = "provider_id", nullable = false, length = 255)
  private String providerId;  // OAuth provider's unique user ID (e.g., Google's sub claim)

  @Column(name = "provider_email", length = 255)
  private String providerEmail;  // Email from the OAuth provider (for reference)

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public UserOAuthProviderEntity(UserEntity user, String provider, String providerId, String providerEmail) {
    this.user = user;
    this.provider = provider;
    this.providerId = providerId;
    this.providerEmail = providerEmail;
  }
}
