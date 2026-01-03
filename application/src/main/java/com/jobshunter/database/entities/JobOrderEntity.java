package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

  @Column(name = "status", columnDefinition = "TEXT")
  @Enumerated(jakarta.persistence.EnumType.STRING)
  private OrderStatus status;

  @Column(name = "notified", nullable = false)
  private boolean notified = false;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @JsonIgnore
  @OneToMany(mappedBy = "jobOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<JobOrderResultEntity> results = new ArrayList<>();

  @PrePersist
  @PreUpdate
  void prePersist() {
    timestamp = Instant.now();
    if (status == null) {
      status = OrderStatus.NEW;
    }
  }

  public JobOrderEntity(UserEntity user, AiModelEntity aiModel, boolean searchCompanies, boolean searchByPrompts) {
    this.user = user;
    this.aiModel = aiModel;
    this.searchCompanies = searchCompanies;
    this.searchByPrompts = searchByPrompts;
  }
}
