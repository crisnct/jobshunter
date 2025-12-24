package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.EngineType;
import com.jobshunter.dto.Job;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserDataService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserJobRepository userJobRepository;

  @Autowired
  private UserPromptRepository userPromptRepository;

  @Autowired
  private UserCvRepository userCvRepository;

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

  @Transactional
  public UserPromptEntity addPrompt(long id, UserEntity user, EngineType engine, String prompt) {
    Objects.requireNonNull(engine);
    UserPromptEntity entity = userPromptRepository.findById(id)
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setJobsFound(0);
          return newEntity;
        });
    if (!entity.getUser().getUsername().equals(user.getUsername())){
      throw new IllegalArgumentException("Wrong id");
    }
    entity.setEngine(engine);
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
  public UserCvEntity replaceUserCv(UserEntity user, byte[] cvContent, String gptFileId, String geminiFileId) {
    UserCvEntity entity = userCvRepository.findByUserId(user.getId())
        .orElseGet(() -> new UserCvEntity(user, cvContent, gptFileId, geminiFileId));
    entity.setCv(cvContent);
    entity.setGptFileId(gptFileId);
    entity.setGeminiFileId(geminiFileId);
    return userCvRepository.save(entity);
  }

  @Transactional
  public void deleteUserCv(UserEntity user) {
    user.setCv(null);
    userCvRepository.deleteByUserId(user.getId());
    userRepository.save(user);
  }

}
