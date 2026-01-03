package com.jobshunter.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_cv")
public class UserCvEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserEntity user;

  @JsonIgnore
  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "cv", nullable = false)
  private byte[] cv;

  @Column(name = "gpt_file_id")
  private String gptFileId;

  @Column(name = "gemini_file_id")
  private String geminiFileId;

  @Column(name = "grok_file_id")
  private String grokFileId;

  public UserCvEntity(UserEntity user, byte[] cv, String gptFileId, String geminiFileId, String grokFileId) {
    this.user = user;
    this.cv = cv;
    this.gptFileId = gptFileId;
    this.geminiFileId = geminiFileId;
    this.grokFileId = grokFileId;
  }
}

