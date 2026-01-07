package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_order")
public class JobOrderEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "engine_configuration_id", nullable = false)
  private AiModelEntity aiModel;

  @Column(name = "search_companies", nullable = false)
  private boolean searchCompanies = false;

  @Column(name = "search_by_prompts", nullable = false)
  private boolean searchByPrompts = false;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(jakarta.persistence.EnumType.STRING)
  private OrderStatus status;

  @Column(name = "notified", nullable = false)
  private boolean notified = false;

  @Column(name = "modified_at", nullable = false)
  private Instant modifiedAt;

  @Column(name = "locked_by", length = 64)
  private String lockedBy;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  public JobOrderEntity(UserEntity user, AiModelEntity aiModel, boolean searchCompanies, boolean searchByPrompts) {
    this.user = user;
    this.aiModel = aiModel;
    this.searchCompanies = searchCompanies;
    this.searchByPrompts = searchByPrompts;
  }

  @PrePersist
  @PreUpdate
  void prePersist() {
    modifiedAt = Instant.now();
    if (status == null) {
      status = OrderStatus.NEW;
    }
  }
}
