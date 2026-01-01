package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
  private EngineConfigurationEntity engineConfiguration;

  @Column(name = "search_companies", nullable = false)
  private boolean searchCompanies = false;

  @Column(name = "status", columnDefinition = "TEXT")
  private String status;

  @Column(name = "notified", nullable = false)
  private boolean notified = false;

  @Column(name = "timestamp", nullable = false)
  private LocalDateTime timestamp;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @JsonIgnore
  @OneToMany(mappedBy = "jobOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<JobOrderResultEntity> results = new ArrayList<>();

  @PrePersist
  void prePersist() {
    if (timestamp == null) {
      timestamp = LocalDateTime.now();
    }
  }

  public JobOrderEntity(UserEntity user, EngineConfigurationEntity engineConfiguration, boolean searchCompanies) {
    this.user = user;
    this.engineConfiguration = engineConfiguration;
    this.searchCompanies = searchCompanies;
  }
}
