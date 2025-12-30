package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

  @Column(name = "modified_at", nullable = false)
  private Instant modifiedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engine_id")
  private EngineConfigurationEntity engineConfiguration;

  @JsonIgnore
  @OneToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "prompts_jobs",
      joinColumns = @JoinColumn(name = "prompt_id"),
      inverseJoinColumns = @JoinColumn(name = "user_jobs_id")
  )
  private List<UserJobEntity> jobs = new ArrayList<>();

  @PrePersist
  @PreUpdate
  void updateModifiedAt() {
    this.modifiedAt = Instant.now();
  }

}
