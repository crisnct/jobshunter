package com.jobshunter.database.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [Issue #46] Junction table linking a user to a language they speak.
 * Used by the validation pipeline to filter job postings by language requirements.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_languages")
public class UserLanguageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "language_id", nullable = false)
  private LanguageEntity language;

  public UserLanguageEntity(UserEntity user, LanguageEntity language) {
    this.user = user;
    this.language = language;
  }
}
