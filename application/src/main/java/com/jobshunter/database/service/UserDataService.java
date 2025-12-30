package com.jobshunter.database.service;

import com.jobshunter.database.entities.EngineConfigurationEntity;
import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.EngineConfigurationRepository;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDataService {

  private final UserRepository userRepository;

  private final UserJobRepository userJobRepository;

  private final UserPromptRepository userPromptRepository;

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

  public void addJobUrl(UserEntity user, String url, EngineConfigurationEntity engineConfiguration) {
    if (!userJobRepository.existsByUserIdAndUrl(user.getId(), url)) {
      userJobRepository.save(new UserJobEntity(user, url, engineConfiguration));
    }
  }

  @Transactional
  public void updateUser(UserEntity user, List<Job> jobs) {
    user.setLastJobs(LocalDateTime.now());
    updateUser(user);
    jobs.forEach(job -> {
      EngineConfigurationEntity engineConfig = null;
      if (job.getPromptId() != null) {
        engineConfig = userPromptRepository.findById(job.getPromptId())
            .map(UserPromptEntity::getEngineConfiguration)
            .orElse(null);
      }
      addJobUrl(user, job.getUrl(), engineConfig);
    });
  }

  public UserPromptEntity updatePrompt(UserEntity user, EngineType engine, Long promptId, String prompt) {
    List<EngineConfigurationEntity> engineConfigs = engineRepository.findByEngine(engine);
    if (engineConfigs.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No engine found: " + engine);
    }
    // Use the first available configuration for the engine
    EngineConfigurationEntity engineConfig = engineConfigs.get(0);
    UserPromptEntity entity = userPromptRepository
        .findByIdAndUserIdAndEngineConfigurationId(promptId == null ? -1 : promptId, user.getId(), engineConfig.getId())
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngineConfiguration(engineConfig);
          newEntity.setJobsFound(0);
          return newEntity;
        });
    entity.setPrompt(prompt);
    return userPromptRepository.save(entity);
  }

  @Transactional
  public void incrementPromptJobsFound(long promptId, int amount) {
    UserPromptEntity entity = userPromptRepository.findById(promptId).orElseThrow();
    int current = entity.getJobsFound() != null ? entity.getJobsFound() : 0;
    entity.setJobsFound(current + amount);
    userPromptRepository.save(entity);
  }

  @Transactional
  public void saveJobsToDB(Map<Long, List<Job>> jobsByPrompt, UserEntity user) {
    for (Entry<Long, List<Job>> entry : jobsByPrompt.entrySet()) {
      this.incrementPromptJobsFound(entry.getKey(), entry.getValue().size());
    }
    List<Job> jobs = jobsByPrompt.values().stream().flatMap((Function<List<Job>, Stream<Job>>) Collection::stream).toList();
    this.updateUser(user, jobs);
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
