package com.jobshunter.service.application;

import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.dto.JobHuntResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.service.application.hunting.CountryIsoCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobOrderProcessor {

  private final JobHuntService jobHuntService;
  private final UserDBService userDBService;
  private final UserJobDBService userJobDBService;
  private final JobOrderDBService jobOrderDBService;
  private final UserCvService userCvService;
  private final ApplicationProperties properties;
  private final CountryIsoCode countryIsoCode;

  public JobHuntResponse process(Long orderId) {
    JobOrderEntity jobOrder = jobOrderDBService.getJobOrder(orderId);
    String username = jobOrder.getUser().getUsername();
    log.info("Start processing job order id={} for user {}", jobOrder.getId(), username);
    try {
      for (EngineType type : EngineType.values()) {
        if (type.isAiProvider()) {
          userCvService.refreshUserCvIfNeeded(jobOrder.getUser(), type);
        }
      }

      UserEntity user = userDBService.getUserCompleteInfo(username).orElseThrow();

      boolean isEnableOneRealEngine = (properties.getGemini().isEnabled() || properties.getGpt().isEnabled() || properties.getSerp().isEnabled());
      final List<String> ignoredURLs;
      if (isEnableOneRealEngine) {
        ignoredURLs = userJobDBService.getUserJobs(username).stream().map(UserJobEntity::getUrl).toList();
      } else {
        ignoredURLs = List.of();
      }
      SearchJobOrder order = new SearchJobOrder(jobOrder, user, ignoredURLs);
      order.setCountryISOcode(countryIsoCode.getCode(user.getCountry()));

      JobHuntResponse response = jobHuntService.searchJobsForUser(order);
      jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.COMPLETED, null);
      log.info("Completed processing job order id={} for user {}", jobOrder.getId(), username);
      return response;
    } catch (Exception e) {
      log.error("Error processing job order id={} for user {}: {}", jobOrder.getId(), username, e.getMessage(), e);
      jobOrderDBService.changeStatus(jobOrder.getId(), OrderStatus.FAILED, e.getMessage());
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(e);
    }
  }

}
