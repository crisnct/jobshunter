package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_job_types",
    uniqueConstraints = @UniqueConstraint(name = "uc_user_job_type", columnNames = {"user_id", "job_type"}))
public class UserJobTypeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Enumerated(EnumType.STRING)
  @Column(name = "job_type", nullable = false, length = 6)
  private JobType jobType;

  public UserJobTypeEntity(UserEntity user, JobType jobType) {
    this.user = user;
    this.jobType = jobType;
  }
}
