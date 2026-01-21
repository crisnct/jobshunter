package com.jobshunter.database.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_models_capability")
public class AiModelsCapabilityEntity {

  @EmbeddedId
  private AiModelsCapabilityId id;

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("modelId")
  @JoinColumn(name = "model_id", nullable = false)
  private AiModelEntity model;

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("capabilityId")
  @JoinColumn(name = "capability_id", nullable = false)
  private AiCapabilityEntity capability;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "config_json", columnDefinition = "JSON")
  private JsonNode configJson;

  public AiModelsCapabilityEntity(AiModelEntity model, AiCapabilityEntity capability) {
    this.id = new AiModelsCapabilityId(model.getId(), capability.getId());
    this.model = model;
    this.capability = capability;
  }
}
