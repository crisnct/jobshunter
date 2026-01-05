package com.jobshunter.database.service;

import com.jobshunter.database.entities.AiModelEntity;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

  @Transactional
  public UserJobEntity addJobUrl(UserEntity user, String url, AiModelEntity aiModel) {
    Optional<UserJobEntity> existing = userJobRepository.findByUserIdAndUrl(user.getId(), url);
    return existing.orElseGet(() -> userJobRepository.save(new UserJobEntity(user, url, aiModel)));
  }

  @Transactional
  public void updateUserWithJobs(UserEntity user, SearchJobOrder order, List<Job> jobs) {
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    AiModelEntity aiModel
        = aiModelRepository.findByProviderAndModel(order.getEngineSelection().type(), order.getEngineSelection().model()).get();

    user.setLastJobs(Instant.now());
    jobs.forEach(job -> {
      UserPromptEntity userPrompt = null;
      if (job.getPromptId() != null) {
        Optional<UserPromptEntity> promptOptional = userPromptRepository.findById(job.getPromptId());
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
    userDBService.updateUser(user);
  }

  @Transactional(readOnly = true)
  public List<String> getExistingJobUrlsForUser(String username) {
    return userJobRepository.findJobUrlsByUsernameIgnoreCase(username).stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
  }
}
