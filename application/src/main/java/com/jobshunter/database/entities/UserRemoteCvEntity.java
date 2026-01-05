package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.EngineType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_remote_cv")
@IdClass(UserRemoteCvId.class)
public class UserRemoteCvEntity {

  @Id
  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Id
  @Column(name = "provider", nullable = false, length = 25)
  @Enumerated(EnumType.STRING)
  private EngineType provider;

  @Column(name = "file_id", nullable = false, length = 255)
  private String fileId;

  @Column(name = "filename", nullable = false, length = 255)
  private String filename;

  @Column(name = "expire_time")
  private Instant expireTime;

  public UserRemoteCvEntity(UserEntity user, EngineType provider, String fileId, String filename, Instant expireTime) {
    this.user = user;
    this.provider = provider;
    this.fileId = fileId;
    this.filename = filename;
    this.expireTime = expireTime;
  }

  public UserRemoteCvEntity(UserEntity user, EngineType provider, String fileId, String filename) {
    this.user = user;
    this.provider = provider;
    this.fileId = fileId;
    this.filename = filename;
  }
}
