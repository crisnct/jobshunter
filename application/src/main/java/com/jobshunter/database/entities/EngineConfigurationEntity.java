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
@Table(name = "engine_configuration")
@ToString
public class EngineConfigurationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "engine", nullable = false, length = 255)
  private EngineType engine;

  @Column(name = "model", nullable = false, length = 255)
  private String model;

  public EngineConfigurationEntity(EngineType engine, String model) {
    this.engine = engine;
    this.model = model;
  }
}

