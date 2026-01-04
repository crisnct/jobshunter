package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.PromptsJobsEntity;
import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserDeviceEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.entities.UserRemoteCvEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.JobOrderRepository;
import com.jobshunter.database.repository.PromptsJobsRepository;
import com.jobshunter.database.repository.UserContractTypeRepository;
import com.jobshunter.database.repository.UserCvRepository;
import com.jobshunter.database.repository.UserDeviceRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserJobRoleRepository;
import com.jobshunter.database.repository.UserJobTypeRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.database.repository.UserRemoteCvRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.dto.exceptions.BusinessException;
import com.jobshunter.model.ContractType;
import com.jobshunter.model.EngineCategory;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.Job;
import com.jobshunter.model.JobType;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.ResumeFileInfo;
import com.jobshunter.model.SearchJobOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  private final AiModelRepository aiModelRepository;

  private final UserCvRepository userCvRepository;

  private final UserDeviceRepository userDeviceRepository;

  private final UserJobRoleRepository userJobRoleRepository;

  private final UserJobTypeRepository userJobTypeRepository;

  private final UserContractTypeRepository userContractTypeRepository;

  private final JobOrderRepository jobOrderRepository;

  private final UserRemoteCvRepository userRemoteCvRepository;

  public List<UserJobEntity> getUserJobs(String username) {
    return userJobRepository.findAllByUsernameWithUser(username);
  }

  @Transactional(readOnly = true)
  public List<UserEntity> getAllUsers() {
    List<UserEntity> all = userRepository.findAll();
    for (UserEntity user : all) {
      initializeUserData(user);
    }
    return all;
  }

  private void initializeUserData(UserEntity user) {
    Hibernate.initialize(user.getPrompts());
    Hibernate.initialize(user.getRoles());
    if (user.getCv() != null) {
      Hibernate.initialize(user.getCv());
      if (user.getCv().getRemoteCvs() != null) {
        Hibernate.initialize(user.getCv().getRemoteCvs());
      }
    }
    Hibernate.initialize(user.getJobRoles());
    Hibernate.initialize(user.getJobTypes());
    Hibernate.initialize(user.getContractTypes());
  }

  public Optional<UserEntity> getUser(String username) {
    return userRepository.findByUsername(username);
  }

  @Transactional(readOnly = true)
  public Optional<UserEntity> getUserCompleteInfo(String username) {
    Optional<UserEntity> userop = userRepository.findByUsername(username);
    userop.ifPresent(this::initializeUserData);
    return userop;
  }

  @SuppressWarnings("UnusedReturnValue")
  public UserEntity updateUser(UserEntity user) {
    return userRepository.save(user);
  }

  public UserJobEntity addJobUrl(UserEntity user, String url, AiModelEntity aiModel) {
    Optional<UserJobEntity> existing = userJobRepository.findByUserIdAndUrl(user.getId(), url);
    return existing.orElseGet(() -> userJobRepository.save(new UserJobEntity(user, url, aiModel)));
  }

  @Transactional
  public void updateUser(UserEntity user, SearchJobOrder order, List<Job> jobs) {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    AiModelEntity aiModel
        = aiModelRepository.findByProviderAndModel(order.getEngineSelection().type(), order.getEngineSelection().model()).get();

    user.setLastJobs(Instant.now());
    jobs.forEach(job -> {
      UserPromptEntity userPrompt = null;
      if (job.getPromptId() != null) {
        var promptOptional = userPromptRepository.findById(job.getPromptId());
        if (promptOptional.isPresent()) {
          userPrompt = promptOptional.get();
        }
      }
      UserJobEntity userJobEntity = addJobUrl(user, job.getUrl(), aiModel);
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

  public UserPromptEntity updatePrompt(UserEntity user, EngineCategory category, Long promptId, String prompt) {
    // Use the first available configuration for the engine
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
  public UserCvEntity replaceUserCv(UserEntity user, byte[] cvContent, Map<EngineType, ResumeFileInfo> result) {
    UserCvEntity entity = userCvRepository.findByUserId(user.getId())
        .orElseGet(() -> new UserCvEntity(user, cvContent));
    entity.setCv(cvContent);
    entity = userCvRepository.save(entity);
    for (Entry<EngineType, ResumeFileInfo> entry : result.entrySet()) {
      this.saveRemoteCvFile(entity, entry.getKey(), entry.getValue());
    }
    return entity;
  }

  @Transactional
  public void saveRemoteCvFile(
      UserCvEntity userCv,
      EngineType provider,
      ResumeFileInfo fileInfo
  ) {
    UserRemoteCvEntity entity = userRemoteCvRepository.findByUserCvIdAndProvider(userCv.getId(), provider)
        .orElse(new UserRemoteCvEntity(userCv, provider, fileInfo.fileId(), fileInfo.filename()));
    entity.setFileId(fileInfo.fileId());
    entity.setFilename(fileInfo.filename());
    entity.setExpireTime(fileInfo.expireAt());
    userRemoteCvRepository.save(entity);
  }

  public Optional<String> getRemoteCvFileId(UserCvEntity userCv, EngineType provider) {
    if (userCv == null || userCv.getId() == null) {
      return Optional.empty();
    }
    return userRemoteCvRepository.findByUserCvIdAndProvider(userCv.getId(), provider)
        .map(UserRemoteCvEntity::getFileId);
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
  public JobOrderEntity createJobOrder(UserEntity user, Long engineConfigurationId, boolean searchCompanies, boolean searchByPrompts) {
    AiModelEntity aiModel = aiModelRepository.findById(engineConfigurationId)
        .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
            "AI model with id " + engineConfigurationId + " not found"));

    JobOrderEntity jobOrder = new JobOrderEntity(user, aiModel, searchCompanies, searchByPrompts);
    return jobOrderRepository.save(jobOrder);
  }

  @Transactional
  public void saveJobOrder(JobOrderEntity jobOrder) {
    jobOrderRepository.save(jobOrder);
  }

  @Transactional(readOnly = true)
  public List<JobOrderEntity> getUserOrders(Long userId) {
    List<JobOrderEntity> orders = jobOrderRepository.findByUserIdOrderByTimestampDescAndStatus(userId);
    orders.forEach(order -> Hibernate.initialize(order.getAiModel()));
    return orders;
  }

  @Transactional(readOnly = true)
  public Optional<JobOrderEntity> getUserOldestNewOrder() {
    Optional<JobOrderEntity> lastOrder = jobOrderRepository.findOldestByStatus(OrderStatus.NEW);
    if (lastOrder.isEmpty()) {
      return Optional.empty();
    }
    initializeUserData(lastOrder.get().getUser());
    Hibernate.initialize(lastOrder.get().getAiModel());
    return lastOrder;
  }

  @Transactional
  public void updateDeviceId(String username, String deviceId, String ip, String userAgent) {
    UserDeviceEntity entity = userDeviceRepository.findByUsername(username).orElseGet(() -> {
      UserDeviceEntity newEntity = new UserDeviceEntity();
      userRepository.findByUsername(username).ifPresent(newEntity::setUser);
      return newEntity;
    });
    entity.setDeviceId(deviceId);
    entity.setIpAddress(ip);
    entity.setUserAgent(userAgent);
    userDeviceRepository.save(entity);
  }

  @Transactional(readOnly = true)
  public Optional<UserDeviceEntity> getActiveDevice(String username) {
    return userDeviceRepository.findActiveByUsername(username);
  }
}
