package com.jobshunter.database.entities;

import com.jobshunter.model.EngineType;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserRemoteCvId implements Serializable {

  private Long user;

  private EngineType provider;
}