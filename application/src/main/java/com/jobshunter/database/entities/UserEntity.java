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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uc_user_email", columnNames = "email"),
        @UniqueConstraint(name = "uc_user_verification_token", columnNames = "verification_token")
    })
public class UserEntity implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String username;

  @Column(nullable = false)
  private String email;

  @JsonIgnore
  @Column(name = "password_hash", nullable = false)
  private String password;

  @Column(name = "phone_number", nullable = false, length = 30)
  private String phoneNumber;

  @Column(name = "notify_whatsapp", nullable = false)
  private boolean notifyWhatsapp = false;

  @Column(name = "notify_email", nullable = false)
  private boolean notifyEmail = false;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "verification_token")
  private String verificationToken;

  @Column(name = "cv_file_id")
  private String cvFileId;

  @Column(name = "last_jobs")
  private LocalDateTime lastJobs;

  @Column(name = "time_interval")
  private Integer timeInterval;

  @Column(name = "prompt", length = 500)
  private String prompt;

  @Column(name = "serp_api_request")
  private String serpApiRequest;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "approved", nullable = false)
  private boolean approved = false;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<RoleEntity> roles = new HashSet<>();

  @JsonIgnore
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserJobEntity> jobs = new ArrayList<>();

  @JsonIgnore
  @Column(name = "jwt_token", length = 64)
  private String jwtToken;

  @Override
  @JsonIgnore
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
        .toList();
  }

  @Override
  @JsonIgnore
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  @JsonIgnore
  public boolean isEnabled() {
    return emailVerified;
  }
}
