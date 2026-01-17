package com.jobshunter.database.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AiModelsCapabilityId implements Serializable {

  @Column(name = "model_id")
  private Long modelId;

  @Column(name = "capability_id")
  private Long capabilityId;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AiModelsCapabilityId that = (AiModelsCapabilityId) o;
    return Objects.equals(modelId, that.modelId) && Objects.equals(capabilityId, that.capabilityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(modelId, capabilityId);
  }
}
