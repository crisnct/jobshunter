package com.jobshunter.database.entities;

import com.jobshunter.model.EngineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_models")
@ToString
public class AiModelEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 255)
  private EngineType provider;

  @Column(name = "model", nullable = false, length = 255)
  private String model;

  @Column(name = "context_window")
  private Integer contextWindow;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  public AiModelEntity(EngineType provider, String model) {
    this.provider = provider;
    this.model = model;
  }
}
