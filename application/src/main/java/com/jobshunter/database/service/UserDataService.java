package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.service.application.JobsSynchronizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  private final UserCvRepository userCvRepository;

  public List<UserJobEntity> getUserJobs(String username) {
    return userJobRepository.findAllByUsernameWithUser(username);
  }

  public List<UserEntity> getAllUsers() {
    return userRepository.findAllWithPrompts();
  }

  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsernameWithPrompts(username);
  }

  @SuppressWarnings("UnusedReturnValue")
  public UserEntity updateUser(UserEntity user) {
    return userRepository.save(user);
  }

  public void addJobUrl(UserEntity user, String url) {
    if (!userJobRepository.existsByUserIdAndJobUrl(user.getId(), url)) {
      userJobRepository.save(new UserJobEntity(user, url));
    }
  }

  @Transactional
  public void updateUser(UserEntity user, List<Job> jobs) {
    user.setLastJobs(LocalDateTime.now());
    updateUser(user);
    jobs.forEach(job -> addJobUrl(user, job.getUrl()));
  }

  public UserPromptEntity updatePrompt(UserEntity user, EngineType engine, String prompt) {
    UserPromptEntity entity = userPromptRepository.findByUserIdAndPromptIgnoreCaseAndEngine(user.getId(), prompt, engine)
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngine(engine);
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
  public void saveJobsToDB(JobsSynchronizer jobsSync, UserEntity user, List<Job> validatedJobs) {
    log.info("Found {} jobs for {} ", validatedJobs.size(), user.getEmail());
    validatedJobs.forEach(v -> log.info(v.getUrl()));
    for (Entry<Long, List<Job>> entry : jobsSync.getJobs().entrySet()) {
      int count = (int) entry.getValue().stream().filter(validatedJobs::contains).count();
      this.incrementPromptJobsFound(entry.getKey(), count);
    }
    this.updateUser(user, validatedJobs);
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
