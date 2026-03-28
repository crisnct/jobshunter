package com.jobshunter.database.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
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
