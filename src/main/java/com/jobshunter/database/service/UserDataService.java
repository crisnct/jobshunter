package com.jobshunter.database.service;

import com.jobshunter.database.entities.User;
import com.jobshunter.database.entities.UserJob;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class UserDataService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserJobRepository userJobRepository;

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public Optional<User> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  @SuppressWarnings("UnusedReturnValue")
  public User updateUser(User user) {
    return userRepository.save(user);
  }

  public void addJobUrl(User user, String url) {
    if (!userJobRepository.existsByUserIdAndJobUrl(user.getId(), url)) {
      userJobRepository.save(new UserJob(user, url));
    }
  }

  public List<String> getExistingJobUrlsForUser(String username) {
    return userJobRepository.findJobUrlsByUsernameIgnoreCase(username).stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .collect(Collectors.toList());
  }

}
