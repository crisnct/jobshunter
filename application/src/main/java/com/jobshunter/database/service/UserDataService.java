package com.jobshunter.database.service;

import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserDeviceEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobType;
import com.jobshunter.model.ResumeFileInfo;
import com.jobshunter.model.SearchJobOrder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service that delegates to specialized services.
 * Maintained for backward compatibility during migration.
 * Prefer using the specific services directly in new code:
 * - UserService for core user operations
 * - UserJobService for job management
 * - UserPromptService for prompt management
 * - UserCvDataService for CV management
 * - UserProfileService for profile preferences
 * - UserDeviceService for device management
 * - JobOrderService for job orders
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserDataService {

  private final UserService userService;
  private final UserJobService userJobService;
  private final UserPromptService userPromptService;
  private final UserCvDataService userCvDataService;
  private final UserProfileService userProfileService;
  private final UserDeviceService userDeviceService;
  private final JobOrderService jobOrderService;

  // ========== User Operations (delegated to UserService) ==========

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUser(String username) {
    return userService.getUser(username);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getAllUsers() {
    return userService.getAllUsers();
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserCompleteInfo(String username) {
    return userService.getUserCompleteInfo(username);
  }

  @Transactional
  public UserEntity updateUser(UserEntity user) {
    return userService.updateUser(user);
  }

  @Transactional
  public void save(UserEntity user) {
    userService.save(user);
  }

  @Transactional
  public void deleteUserByUsername(String username) {
    userService.deleteUserByUsername(username);
  }

  // ========== User Job Operations (delegated to UserJobService) ==========

  @Transactional(readOnly = true)
  public List<UserJobEntity> getUserJobs(String username) {
    return userJobService.getUserJobs(username);
  }

  @Transactional
  public void updateUser(UserEntity user, SearchJobOrder order, List<Job> jobs) {
    userJobService.updateUserWithJobs(user, order, jobs);
  }

  @Transactional(readOnly = true)
  public List<String> getExistingJobUrlsForUser(String username) {
    return userJobService.getExistingJobUrlsForUser(username);
  }

  // ========== User Prompt Operations (delegated to UserPromptService) ==========

  @Transactional
  public UserPromptEntity updatePrompt(UserEntity user, EngineCategory category, Long promptId, String prompt) {
    return userPromptService.updatePrompt(user, category, promptId, prompt);
  }

  @Transactional
  public void deleteUserPrompts(List<Long> prompts) {
    userPromptService.deleteUserPrompts(prompts);
  }

  // ========== User CV Operations (delegated to UserCvDataService) ==========

  @Transactional(readOnly = true)
  public Optional<UserCvEntity> getUserCv(String username) {
    return userCvDataService.getUserCv(username);
  }

  @Transactional
  public UserCvEntity replaceUserCv(UserEntity user, byte[] cvContent, Map<EngineType, ResumeFileInfo> result) {
    return userCvDataService.replaceUserCv(user, cvContent, result);
  }

  @Transactional
  public void saveRemoteCvFile(UserCvEntity userCv, EngineType provider, ResumeFileInfo fileInfo) {
    userCvDataService.saveRemoteCvFile(userCv, provider, fileInfo);
  }

  @Transactional(readOnly = true)
  public Optional<String> getRemoteCvFileId(UserCvEntity userCv, EngineType provider) {
    return userCvDataService.getRemoteCvFileId(userCv, provider);
  }

  @Transactional
  public void deleteUserCv(UserEntity user) {
    userCvDataService.deleteUserCv(user);
    userService.updateUser(user);
  }

  // ========== User Profile Operations (delegated to UserProfileService) ==========

  @Transactional
  public void updateUserJobRoles(UserEntity user, List<String> jobRoles) {
    userProfileService.updateUserJobRoles(user, jobRoles);
  }

  @Transactional
  public void updateUserJobTypes(UserEntity user, List<JobType> jobTypes) {
    userProfileService.updateUserJobTypes(user, jobTypes);
  }

  @Transactional
  public void updateUserContractTypes(UserEntity user, List<ContractType> contractTypes) {
    userProfileService.updateUserContractTypes(user, contractTypes);
  }

  // ========== Job Order Operations (delegated to JobOrderService) ==========

  @Transactional
  public JobOrderEntity createJobOrder(UserEntity user, Long engineConfigurationId, boolean searchCompanies, boolean searchByPrompts) {
    return jobOrderService.createJobOrder(user, engineConfigurationId, searchCompanies, searchByPrompts);
  }

  @Transactional
  public void saveJobOrder(JobOrderEntity jobOrder) {
    jobOrderService.saveJobOrder(jobOrder);
  }

  @Transactional(readOnly = true)
  public List<JobOrderEntity> getUserOrders(Long userId) {
    return jobOrderService.getUserOrders(userId);
  }

  @Transactional(readOnly = true)
  public Optional<JobOrderEntity> getUserOldestNewOrder() {
    return jobOrderService.getUserOldestNewOrder();
  }

  // ========== User Device Operations (delegated to UserDeviceService) ==========

  @Transactional
  public void updateDeviceId(String username, String deviceId, String ip, String userAgent) {
    userDeviceService.updateDeviceId(username, deviceId, ip, userAgent);
  }

  @Transactional(readOnly = true)
  public Optional<UserDeviceEntity> getActiveDevice(String username) {
    return userDeviceService.getActiveDevice(username);
  }
}
