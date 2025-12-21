package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.Job;
import java.time.LocalDateTime;
import java.util.List;
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

  public List<UserJobEntity> getUserJobs(String username) {
    return userJobRepository.findAllByUsernameWithUser(username);
  }

  public List<UserEntity> getAllUsers() {
    return userRepository.findAll();
  }

  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsername(username);
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
    jobs.forEach(job -> addJobUrl(user, job.url()));
  }

  @Transactional
  public UserPromptEntity addPrompt(UserEntity user, String engine, String prompt) {
    String normalizedEngine = normalize(engine);
    UserPromptEntity entity = userPromptRepository.findByUserIdAndEngine(user.getId(), engine)
        .orElseGet(() -> {
          UserPromptEntity newEntity = new UserPromptEntity();
          newEntity.setUser(user);
          newEntity.setEngine(normalizedEngine);
          newEntity.setJobsFound(0);
          return newEntity;
        });
    entity.setPrompt(prompt);
    return userPromptRepository.save(entity);
  }

  @Transactional
  public void incrementPromptJobsFound(UserEntity user, String engine, int amount) {
    UserPromptEntity entity = userPromptRepository.findByUserIdAndEngine(user.getId(), engine)
        .orElseThrow();
    int current = entity.getJobsFound() != null ? entity.getJobsFound() : 0;
    entity.setJobsFound(current + amount);
    userPromptRepository.save(entity);
  }

  private String normalize(String engine) {
    return engine != null ? engine.trim() : "";
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

}
