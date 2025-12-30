package com.jobshunter.database.service;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.entities.PromptsJobsEntity;
import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.EngineConfigurationRepository;
import com.jobshunter.database.repository.PromptsJobsRepository;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDataService {

  private final UserRepository userRepository;

  private final UserJobRepository userJobRepository;

  private final UserPromptRepository userPromptRepository;

  private final PromptsJobsRepository promptsJobsRepository;

  private final EngineConfigurationRepository engineRepository;

  private final UserCvRepository userCvRepository;

  public List<UserJobEntity> getUserJobs(String username) {
    return userJobRepository.findAllByUsernameWithUser(username);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getAllUsers() {
    List<UserEntity> all = userRepository.findAll();
    for (UserEntity user : all) {
      Hibernate.initialize(user.getPrompts());
      Hibernate.initialize(user.getRoles());
      user.getPrompts().forEach(p -> Hibernate.initialize(p.getEngineConfiguration()));
      Hibernate.initialize(user.getCv());
    }
    return all;
  }

  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserCompleteInfo(String username) {
    Optional<UserEntity> userop = userRepository.findByUsername(username);
    if (userop.isPresent()) {
      UserEntity entity = userop.get();
      Hibernate.initialize(entity.getPrompts());
      entity.getPrompts().forEach(p -> Hibernate.initialize(p.getEngineConfiguration()));
      Hibernate.initialize(entity.getCv());
    }
    return userop;
  }

  @SuppressWarnings("UnusedReturnValue")
  public UserEntity updateUser(UserEntity user) {
    return userRepository.save(user);
  }

  public UserJobEntity addJobUrl(UserEntity user, String url, EngineConfigurationEntity engineConfiguration) {
    Optional<UserJobEntity> existing = userJobRepository.findByUserIdAndUrl(user.getId(), url);
    return existing.orElseGet(() -> userJobRepository.save(new UserJobEntity(user, url, engineConfiguration)));
  }

  @Transactional
  public void updateUser(UserEntity user, List<Job> jobs) {
    user.setLastJobs(LocalDateTime.now());
    jobs.forEach(job -> {
      EngineConfigurationEntity engineConfig = null;
      UserPromptEntity userPrompt = null;
      if (job.getPromptId() != null) {
        var promptOptional = userPromptRepository.findById(job.getPromptId());
        if (promptOptional.isPresent()) {
          userPrompt = promptOptional.get();
          engineConfig = userPrompt.getEngineConfiguration();
        }
      }
      UserJobEntity userJobEntity = addJobUrl(user, job.getUrl(), engineConfig);
      if (userPrompt != null) {
        PromptsJobsEntity promptJob = new PromptsJobsEntity();
        promptJob.setUserJob(userJobEntity);
        promptJob.setPrompt(userPrompt);
        promptsJobsRepository.save(promptJob);
        userPromptRepository.save(userPrompt);
      }
    });
    this.updateUser(user);
  }

  public UserPromptEntity updatePrompt(UserEntity user, EngineType engine, String model, Long promptId, String prompt) {
    EngineConfigurationEntity engineConfig = engineRepository.findByEngineAndModel(engine, model)
        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "No model " + model + " found for engine " + engine));
    // Use the first available configuration for the engine
    UserPromptEntity entity = userPromptRepository
        .findByIdAndUserIdAndEngineConfigurationId(promptId == null ? -1 : promptId, user.getId(), engineConfig.getId())
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngineConfiguration(engineConfig);
          return newEntity;
        });
    entity.setPrompt(prompt);
    return userPromptRepository.save(entity);
  }

  public List<String> getExistingJobUrlsForUser(String username) {
    return userJobRepository.findJobUrlsByUsernameIgnoreCase(username).stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
  }

  public void save(UserEntity user) {
    userRepository.save(user);
  }

  @Transactional
  public void deleteUserByUsername(String username) {
    userRepository.findByUsername(username).ifPresent(userRepository::delete);
  }

  public Optional<UserCvEntity> getUserCv(String username) {
    return userCvRepository.findByUserUsernameIgnoreCase(username);
  }

  @Transactional
  public UserCvEntity replaceUserCv(UserEntity user, byte[] cvContent, String gptFileId) {
    UserCvEntity entity = userCvRepository.findByUserId(user.getId())
        .orElseGet(() -> new UserCvEntity(user, cvContent, gptFileId, null));
    entity.setCv(cvContent);
    entity.setGptFileId(gptFileId);
    return userCvRepository.save(entity);
  }

  @Transactional
  public void deleteUserCv(UserEntity user) {
    user.setCv(null);
    userCvRepository.deleteByUserId(user.getId());
    userRepository.save(user);
  }

  public void deleteUserPrompts(List<Long> prompts) {
    userPromptRepository.deleteAllByIdInBatch(prompts);
  }

}
