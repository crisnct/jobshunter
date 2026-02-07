package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.EngineCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_prompts")
public class UserPromptEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "prompt", nullable = false, length = 3000)
  private String prompt;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "engine_category", length = 10)
  private EngineCategory engineCategory;

  @Column(name = "modified_at", nullable = false)
  private Instant modifiedAt;

  @PrePersist
  @PreUpdate
  void updateModifiedAt() {
    this.modifiedAt = Instant.now();
  }

}
