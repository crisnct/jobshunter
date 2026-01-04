package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.repository.UserContractTypeRepository;
import com.jobshunter.database.repository.UserJobRoleRepository;
import com.jobshunter.database.repository.UserJobTypeRepository;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserJobRoleRepository userJobRoleRepository;
  private final UserJobTypeRepository userJobTypeRepository;
  private final UserContractTypeRepository userContractTypeRepository;

  @Transactional
  public void updateUserJobRoles(UserEntity user, List<String> jobRoles) {
    userJobRoleRepository.deleteByUserId(user.getId());
    user.getJobRoles().clear();
    if (jobRoles != null) {
      jobRoles.forEach(jobRole -> {
        if (jobRole != null && !jobRole.trim().isEmpty() && jobRole.length() <= 35) {
          UserJobRoleEntity entity = new UserJobRoleEntity(user, jobRole.trim());
          user.getJobRoles().add(entity);
        }
      });
    }
  }

  @Transactional
  public void updateUserJobTypes(UserEntity user, List<JobType> jobTypes) {
    userJobTypeRepository.deleteByUserId(user.getId());
    user.getJobTypes().clear();
    if (jobTypes != null) {
      jobTypes.forEach(jobType -> {
        if (jobType != null) {
          UserJobTypeEntity entity = new UserJobTypeEntity(user, jobType);
          user.getJobTypes().add(entity);
        }
      });
    }
  }

  @Transactional
  public void updateUserContractTypes(UserEntity user, List<ContractType> contractTypes) {
    userContractTypeRepository.deleteByUserId(user.getId());
    user.getContractTypes().clear();
    if (contractTypes != null) {
      contractTypes.forEach(contractType -> {
        if (contractType != null) {
          UserContractTypeEntity entity = new UserContractTypeEntity(user, contractType);
          user.getContractTypes().add(entity);
        }
      });
    }
  }
}
