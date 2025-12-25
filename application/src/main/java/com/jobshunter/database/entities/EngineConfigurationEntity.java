package com.jobshunter.database.entities;

import com.jobshunter.model.EngineTier;
import com.jobshunter.model.EngineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "engine_configuration",
    uniqueConstraints = @UniqueConstraint(name = "uc_engine_configuration_type_tier", columnNames = {"engine_type", "tier"})
)
public class EngineConfigurationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "engine_type", nullable = false, length = 255)
  private EngineType engineType;

  @Enumerated(EnumType.STRING)
  @Column(name = "tier", nullable = false, length = 255)
  private EngineTier tier;

  @Column(name = "model", nullable = false, length = 255)
  private String model;

  public EngineConfigurationEntity(EngineType engineType, EngineTier tier, String model) {
    this.engineType = engineType;
    this.tier = tier;
    this.model = model;
  }
}

