package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_jobs",
    uniqueConstraints = @UniqueConstraint(name = "uc_user_jobs_user_url", columnNames = {"user_id", "url"}))
public class UserJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engine_configuration_id")
    private EngineConfigurationEntity engineConfiguration;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UserJobEntity(UserEntity user, String url) {
        this.user = user;
        this.url = url;
    }

    public UserJobEntity(UserEntity user, String url, EngineConfigurationEntity engineConfiguration) {
        this.user = user;
        this.url = url;
        this.engineConfiguration = engineConfiguration;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
