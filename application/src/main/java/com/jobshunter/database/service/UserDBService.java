package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserSessionEntity;
import com.jobshunter.database.repository.UserContractTypeRepository;
import com.jobshunter.database.repository.UserJobRoleRepository;
import com.jobshunter.database.repository.UserJobTypeRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.database.repository.UserSessionRepository;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.JobType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDBService {

  private final UserRepository userRepository;
  private final UserJobRoleRepository userJobRoleRepository;
  private final UserJobTypeRepository userJobTypeRepository;
  private final UserContractTypeRepository userContractTypeRepository;
  private final UserPromptRepository userPromptRepository;
  private final UserSessionRepository userSessionRepository;

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getAllUsers() {
    List<UserEntity> all = userRepository.findAll();
    for (UserEntity user : all) {
      initialize(user);
    }
    return all;
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserCompleteInfo(String username) {
    Optional<UserEntity> userOptional = userRepository.findByUsername(username);
    userOptional.ifPresent(this::initialize);
    return userOptional;
  }

  @Transactional
  public UserEntity updateUser(UserEntity user) {
    return userRepository.save(user);
  }

  @Transactional
  public void save(UserEntity user) {
    userRepository.save(user);
  }

  @Transactional
  public void deleteUserByUsername(String username) {
    userRepository.findByUsername(username).ifPresent(userRepository::delete);
  }

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

  @Transactional
  public UserPromptEntity updatePrompt(UserEntity user, EngineCategory category, Long promptId, String prompt) {
    UserPromptEntity entity = userPromptRepository
        .findByIdAndUserId(promptId == null ? -1 : promptId, user.getId())
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngineCategory(category);
          return newEntity;
        });
    entity.setPrompt(prompt);
    return userPromptRepository.save(entity);
  }

  @Transactional
  public void deleteUserPrompts(List<Long> prompts) {
    userPromptRepository.deleteAllByIdInBatch(prompts);
  }


  /**
   * Gets the active device ID for a user from their active session.
   *
   * @param username The username
   * @return Optional device ID if user has an active session
   */
  @Transactional(readOnly = true)
  public Optional<String> getActiveDeviceId(String username) {
    return userRepository.findByUsername(username)
        .flatMap(user -> userSessionRepository.findByUser(user)
            .map(UserSessionEntity::getDeviceId));
  }

  /**
   * Initializes lazy-loaded associations for a user entity. This method ensures that related entities are loaded before the transaction ends.
   */
  public void initialize(UserEntity user) {
    Hibernate.initialize(user.getPrompts());
    Hibernate.initialize(user.getRoles());
    Hibernate.initialize(user.getCv());
    Hibernate.initialize(user.getRemoteCvs());
    Hibernate.initialize(user.getJobRoles());
    Hibernate.initialize(user.getJobTypes());
    Hibernate.initialize(user.getContractTypes());
  }
}
