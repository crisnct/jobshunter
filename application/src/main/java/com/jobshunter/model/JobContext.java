package com.jobshunter.model;

import com.jobshunter.database.entities.UserContractTypeEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.database.entities.UserJobTypeEntity;
import com.jobshunter.database.entities.UserLanguageEntity;
import com.jobshunter.service.clients.browser.HttpFetchResult;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Data
@Slf4j
public class JobContext {

  private final Job job;
  private SearchJobOrder order;

  private final String username;
  private final String city;
  private final String country;
  private String body;
  private HttpFetchResult fetchResult;
  private String host;
  private boolean validatedSuccessfully;
  private JobPhase phase;
  private boolean failed;
  private String finalizationMessage;
  private final List<JobType> userJobTypes;
  private final List<ContractType> userContractTypes;
  private final List<String> userRoles;
  // [Issue #46] List of language names the user speaks, used by LanguageMatchRule during validation
  private final List<String> userLanguages;

  public JobContext(Job job, UserEntity user, SearchJobOrder order) {
    this.job = job;
    this.phase = JobPhase.NEW;
    this.order = order;
    this.username = user.getUsername();
    this.city = user.getCity();
    this.country = user.getCountry();
    this.userJobTypes = user.getJobTypes().stream().map(UserJobTypeEntity::getJobType).toList();
    this.userContractTypes = user.getContractTypes().stream().map(UserContractTypeEntity::getContractType).toList();
    this.userRoles = user.getJobRoles().stream().map(UserJobRoleEntity::getJobRole).toList();
    // [Issue #46] Extract language names from user's language entities for validation filtering
    this.userLanguages = user.getLanguages().stream().map(lang -> lang.getLanguage().getName()).toList();
  }

  public static JobContext failed(Job job, UserEntity user, Throwable t) {
    JobContext ctx = new JobContext(job, user, null);
    ctx.validatedSuccessfully = false;
    ctx.failJob(t != null ? t.getMessage() : "Unknown pipeline failure");
    return ctx;
  }

  public boolean isOkToRun(JobPhase phase) {
    Objects.requireNonNull(phase);
    return !failed && this.phase.ordinal() < phase.ordinal();
  }

  public boolean hasFetchResult() {
    return fetchResult != null && Strings.isNotBlank(fetchResult.body());
  }

  public void finalizeJob(String message) {
    this.finalizationMessage = message;
    this.phase = JobPhase.values()[JobPhase.values().length - 1];
  }

  public void failJob(String message) {
    this.failed = true;
    this.finalizationMessage = message;
    this.phase = JobPhase.values()[JobPhase.values().length - 1];
  }

}
