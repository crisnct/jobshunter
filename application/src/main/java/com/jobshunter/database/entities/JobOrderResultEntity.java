package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
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
@Table(name = "job_order_result",
    uniqueConstraints = @UniqueConstraint(name = "uc_job_order_result_order_job", columnNames = {"job_order_id", "job_id"}))
public class JobOrderResultEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_order_id", nullable = false)
  private JobOrderEntity jobOrder;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private UserJobEntity job;

  public JobOrderResultEntity(JobOrderEntity jobOrder, UserJobEntity job) {
    this.jobOrder = jobOrder;
    this.job = job;
  }
}
