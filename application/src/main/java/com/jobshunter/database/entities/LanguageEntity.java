package com.jobshunter.database.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * [Issue #46] Reference table for spoken languages (e.g. English, French, Romanian).
 * Each row represents a unique language that users can declare in their profile.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "languages", uniqueConstraints = @UniqueConstraint(name = "uc_language_name", columnNames = "name"))
public class LanguageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  public LanguageEntity(String name) {
    this.name = name;
  }
}
