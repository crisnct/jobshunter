package com.jobshunter.service.application;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScoring {

  public void calculateScore(Job job, UserEntity user) {

  }

}
