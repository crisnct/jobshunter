package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jobshunter.model.DeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_devices")
public class UserDeviceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserEntity user;

  @Column(name = "device_id", nullable = false, length = 36)
  private String deviceId;

  @Column(name = "user_agent", length = 1024)
  private String userAgent;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DeviceStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  public UserDeviceEntity(UserEntity user, String deviceId, String userAgent, String ipAddress) {
    this.user = user;
    this.deviceId = deviceId;
    this.userAgent = userAgent;
    this.ipAddress = ipAddress;
    this.status = DeviceStatus.ACTIVE;
    this.createdAt = Instant.now();
    this.lastSeenAt = Instant.now();
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (lastSeenAt == null) {
      lastSeenAt = Instant.now();
    }
    if (status == null) {
      status = DeviceStatus.ACTIVE;
    }
    if (deviceId == null) {
      deviceId = UUID.randomUUID().toString();
    }
  }

  @PreUpdate
  void preUpdate() {
    lastSeenAt = Instant.now();
  }
}
