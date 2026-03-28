package com.jobshunter.database.entities;

import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.model.EngineType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_models",
    uniqueConstraints = @UniqueConstraint(name = "uc_provider_model", columnNames = {"provider", "model"}))
@ToString(exclude = "capabilities")
public class AiModelEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 50)
  private EngineType provider;

  @Column(name = "model", nullable = false, length = 255)
  private String model;

  //context_window = input_tokens + output_tokens + tool_tokens
  @Column(name = "context_window")
  private Integer contextWindow;

  @Column(name = "tokens_per_char")
  private Float tokensPerChar;

  /** Price per input token (e.g. USD per 1M tokens). */
  @Column(name = "input_price")
  private Double inputPrice;

  /** Price per output token (e.g. USD per 1M tokens). */
  @Column(name = "output_price")
  private Double outputPrice;

  /** Tool price per 1M of calls */
  @Column(name = "tool_price")
  private Integer toolPrice;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @OneToMany(mappedBy = "model", fetch = FetchType.EAGER)
  private List<AiModelsCapabilityEntity> capabilities = new ArrayList<>();

  public AiModelEntity(EngineType provider, String model) {
    this.provider = provider;
    this.model = model;
  }

  public boolean isEnabledCapability(AiCapabilityType capabilityType) {
    return capabilities.stream()
        .anyMatch(entity -> entity.isEnabled() && entity.getCapability().getType() == capabilityType);
  }

}
