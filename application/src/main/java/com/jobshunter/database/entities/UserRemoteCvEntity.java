package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.EngineType;
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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_remote_cv",
    uniqueConstraints = @UniqueConstraint(name = "uc_user_remote_cv_cv_provider", columnNames = {"user_cv_id", "provider"}))
public class UserRemoteCvEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_cv_id", nullable = false)
  private UserCvEntity userCv;

  @Column(name = "provider", nullable = false, length = 25)
  @Enumerated(EnumType.STRING)
  private EngineType provider;

  @Column(name = "file_id", nullable = false, length = 255)
  private String fileId;

  @Column(name = "filename", nullable = false, length = 255)
  private String filename;

  @Column(name = "expire_time")
  private Instant expireTime;

  public UserRemoteCvEntity(UserCvEntity userCv, EngineType provider, String fileId, String filename, Instant expireTime) {
    this.userCv = userCv;
    this.provider = provider;
    this.fileId = fileId;
    this.filename = filename;
    this.expireTime = expireTime;
  }

  public UserRemoteCvEntity(UserCvEntity userCv, EngineType provider, String fileId, String filename) {
    this.userCv = userCv;
    this.provider = provider;
    this.fileId = fileId;
    this.filename = filename;
  }
}
