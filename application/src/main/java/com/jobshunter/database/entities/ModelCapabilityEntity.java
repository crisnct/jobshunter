package com.jobshunter.database.entities;

import com.jobshunter.model.ModelCapability;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "models_capability")
public class ModelCapabilityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @JoinColumn(name = "model_id", nullable = false)
  private AiModelEntity model;

  @Enumerated(EnumType.STRING)
  @Column(name = "capability", nullable = false, length = 25)
  private ModelCapability capability;

  public ModelCapabilityEntity(AiModelEntity model, ModelCapability capability) {
    this.model = model;
    this.capability = capability;
  }
}
