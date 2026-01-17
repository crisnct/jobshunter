package com.jobshunter.database.entities;

import com.jobshunter.model.AiCapabilityType;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_capability")
public class AiCapabilityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "type", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private AiCapabilityType type;

  @Column(name = "value_type", nullable = false, length = 32)
  private String valueType;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

}
