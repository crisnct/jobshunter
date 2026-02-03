package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.PromptsJobsEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.database.repository.AiModelRepository;
import com.jobshunter.database.repository.PromptsJobsRepository;
import com.jobshunter.database.repository.UserJobRepository;
import com.jobshunter.database.repository.UserPromptRepository;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserJobDBService {

  private final UserJobRepository userJobRepository;
  private final UserPromptRepository userPromptRepository;
  private final PromptsJobsRepository promptsJobsRepository;
  private final AiModelRepository aiModelRepository;
  private final UserDBService userDBService;

  @Transactional(readOnly = true)
  public List<UserJobEntity> getUserJobs(String username) {
    return userJobRepository.findAllByUsernameWithUser(username);
  }

  @Transactional(readOnly = true)
  public List<UserJobEntity> getUserJobs(String username, Long orderId) {
    return userJobRepository.findAllByUsernameWithUserAndOrderId(username, orderId);
  }

  @Transactional(readOnly = true)
  public long countJobsForOrder(Long userId, Long orderId) {
    return userJobRepository.countByUserIdAndJobOrderId(userId, orderId);
  }

  @Transactional(readOnly = true)
  public Map<Long, Long> countJobsForOrders(Long userId, List<Long> orderIds) {
    if (orderIds == null || orderIds.isEmpty()) {
      return Map.of();
    }
    List<Object[]> results = userJobRepository.countJobsByOrderIds(userId, orderIds);
    return results.stream()
        .collect(Collectors.toMap(
            row -> ((Number) row[0]).longValue(),
            row -> ((Number) row[1]).longValue()
        ));
  }

  @Transactional
  public UserJobEntity addJobUrl(UserEntity user, Job job, AiModelEntity aiModel, JobOrderEntity jobOrder, UserPromptEntity userPrompt) {
    Optional<UserJobEntity> existing = userJobRepository.findByUserIdAndUrl(user.getId(), job.getUrl());
    if (existing.isPresent()) {
      log.debug("Job URL already exists for user {}: {} (id: {})", user.getId(), job.getUrl(), existing.get().getId());
      return existing.get();
    }
    UserJobEntity newJob = new UserJobEntity(user, job.getUrl(), aiModel, jobOrder);
    newJob.setScore(job.getScore());
    newJob.setPrompt(userPrompt);
    return userJobRepository.saveAndFlush(newJob);
  }

  @Transactional
  public void updateUserWithJobs(UserEntity user, SearchJobOrder order, List<Job> jobs) {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    AiModelEntity aiModel
        = aiModelRepository.findByProviderAndModel(order.getModel().getProvider(), order.getModel().getModel()).get();
    jobs.forEach(job -> {
      UserPromptEntity userPrompt = null;
      if (job.getPromptId() != null) {
        Optional<UserPromptEntity> promptOptional = userPromptRepository.findById(job.getPromptId());
        if (promptOptional.isPresent()) {
          userPrompt = promptOptional.get();
        }
      }
      UserJobEntity userJobEntity = addJobUrl(user, job, aiModel, order.getJobOrder(), userPrompt);
      if (userPrompt != null) {
        PromptsJobsEntity promptJob = new PromptsJobsEntity();
        promptJob.setUserJob(userJobEntity);
        promptJob.setPrompt(userPrompt);
        promptsJobsRepository.save(promptJob);
        userPromptRepository.save(userPrompt);
      }
    });
    userDBService.updateUser(user);
  }

}
