package com.jobshunter.database.service;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.UserRepository;
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
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getAllUsers() {
    List<UserEntity> all = userRepository.findAll();
    for (UserEntity user : all) {
      initializeUserData(user);
    }
    return all;
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserCompleteInfo(String username) {
    Optional<UserEntity> userOptional = userRepository.findByUsername(username);
    userOptional.ifPresent(this::initializeUserData);
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

  /**
   * Initializes lazy-loaded associations for a user entity. This method ensures that related entities are loaded before the transaction ends.
   */
  public void initializeUserData(UserEntity user) {
    Hibernate.initialize(user.getPrompts());
    Hibernate.initialize(user.getRoles());
    Hibernate.initialize(user.getCv());
    Hibernate.initialize(user.getRemoteCvs());
    Hibernate.initialize(user.getJobRoles());
    Hibernate.initialize(user.getJobTypes());
    Hibernate.initialize(user.getContractTypes());
  }
}
