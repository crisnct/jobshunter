package com.jobshunter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserCvEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.database.service.UserJobDBService;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.SearchJobSnapshotResponse;
import com.jobshunter.dto.SearchJobsHandle;
import com.jobshunter.dto.SearchStepEvent;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.model.OrderStatus;
import com.jobshunter.service.application.JobOrderProcessor;
import com.jobshunter.service.application.progress.OrderProgressStore;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class InternalMcpControllerTest {

  private static final String USERNAME = "user@example.com";

  private UserDBService userDBService;
  private JobOrderDBService jobOrderDBService;
  private UserJobDBService userJobDBService;
  private OrderProgressStore orderProgressStore;
  private JobOrderProcessor jobOrderProcessor;
  private Executor ordersExecutor;
  private InternalMcpController controller;

  @BeforeEach
  void setUp() {
    userDBService = mock(UserDBService.class);
    jobOrderDBService = mock(JobOrderDBService.class);
    userJobDBService = mock(UserJobDBService.class);
    orderProgressStore = mock(OrderProgressStore.class);
    jobOrderProcessor = mock(JobOrderProcessor.class);
    ordersExecutor = Runnable::run;
    controller = new InternalMcpController(
        userDBService, jobOrderDBService, userJobDBService, orderProgressStore, jobOrderProcessor, ordersExecutor);
  }

  @Test
  void shouldReturnForbiddenWhenPrincipalIsNotRegistered() {
    Authentication authentication = new UsernamePasswordAuthenticationToken("unknown@example.com", "n/a");
    when(userDBService.getUserCompleteInfo("unknown@example.com")).thenReturn(Optional.empty());
    when(userDBService.getUserByEmail("unknown@example.com")).thenReturn(Optional.empty());

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class,
        () -> controller.searchJobs(validRequest(), authentication)
    );

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    assertEquals("Authenticated principal is not registered in Jobshunter.", ex.getReason());
  }

  @Test
  void shouldReturnAuthenticatedUserInfoFromInternalMeEndpoint() {
    Authentication authentication = authenticatedAsUser1();

    UserInfoResponse response = controller.me(authentication).getBody();

    assertEquals(USERNAME, response.username());
    assertEquals(USERNAME, response.email());
  }

  @Test
  void shouldEnqueueOneJobOrderPerConfigurationAndReturnJoinedSearchId() {
    Authentication authentication = authenticatedAsUser1();
    when(jobOrderDBService.createJobOrder(any(), any()))
        .thenReturn(jobOrder(101L, EngineType.GROK, "grok-4-1-fast-non-reasoning", OrderStatus.NEW))
        .thenReturn(jobOrder(102L, EngineType.SERP, "google_jobs", OrderStatus.NEW));
    when(jobOrderDBService.tryAcquireSpecificOrder(101L)).thenReturn(true);
    when(jobOrderDBService.tryAcquireSpecificOrder(102L)).thenReturn(false);

    List<JobOrderRequest> requests = List.of(
        new JobOrderRequest(EngineType.GROK, "grok-4-1-fast-non-reasoning", false, true),
        new JobOrderRequest(EngineType.SERP, "google_jobs", false, true)
    );

    SearchJobsHandle handle = controller.searchJobs(requests, authentication).getBody();

    assertEquals("101,102", handle.searchId());
    verify(jobOrderDBService).createJobOrder(any(), eq(requests.get(0)));
    verify(jobOrderDBService).createJobOrder(any(), eq(requests.get(1)));
    verify(jobOrderProcessor).process(101L);
    verify(jobOrderProcessor, never()).process(102L);
  }

  @Test
  void shouldRejectSearchJobsWhenNeitherSearchOptionIsSet() {
    Authentication authentication = authenticatedAsUser1();
    List<JobOrderRequest> requests = List.of(
        new JobOrderRequest(EngineType.GROK, "grok-4-1-fast-non-reasoning", false, false));

    assertThrows(jakarta.validation.ValidationException.class, () -> controller.searchJobs(requests, authentication));
  }

  @Test
  void shouldReturnInProgressSnapshotWhileAnyOrderIsStillRunning() {
    Authentication authentication = authenticatedAsUser1();
    JobOrderEntity done = jobOrder(1L, EngineType.GROK, "grok-4-1-fast-non-reasoning", OrderStatus.COMPLETED);
    JobOrderEntity pending = jobOrder(2L, EngineType.SERP, "google_jobs", OrderStatus.PROCESSING);
    when(orderProgressStore.get(1L)).thenReturn(List.of(new SearchStepEvent("Pipeline received 20 jobs")));
    when(orderProgressStore.get(2L)).thenReturn(List.of(new SearchStepEvent("FETCH started for https://example.com/job")));
    when(jobOrderDBService.getJobOrders(List.of(1L, 2L))).thenReturn(List.of(done, pending));

    SearchJobSnapshotResponse snapshot = controller.searchJobSnapshot("1,2", authentication).getBody();

    assertEquals("IN_PROGRESS", snapshot.status());
    assertThat(snapshot.events()).hasSize(3);
    assertThat(snapshot.events())
        .extracting(SearchStepEvent::message)
        .contains("Pipeline received 20 jobs", "FETCH started for https://example.com/job", "GROK grok-4-1-fast-non-reasoning: completed");
  }

  @Test
  void shouldReturnDoneSnapshotWithAggregatedResultsWhenAllOrdersComplete() {
    Authentication authentication = authenticatedAsUser1();
    JobOrderEntity first = jobOrder(1L, EngineType.GROK, "grok-4-1-fast-non-reasoning", OrderStatus.COMPLETED);
    JobOrderEntity second = jobOrder(2L, EngineType.SERP, "google_jobs", OrderStatus.COMPLETED);
    when(orderProgressStore.get(1L)).thenReturn(List.of());
    when(orderProgressStore.get(2L)).thenReturn(List.of());
    when(jobOrderDBService.getJobOrders(List.of(1L, 2L))).thenReturn(List.of(first, second));
    when(userJobDBService.getUserJobs(USERNAME, 1L))
        .thenReturn(List.of(userJob("https://example.com/job-1", first.getAiModel(), 80)));
    when(userJobDBService.getUserJobs(USERNAME, 2L))
        .thenReturn(List.of(userJob("https://example.com/job-2", second.getAiModel(), 95)));

    SearchJobSnapshotResponse snapshot = controller.searchJobSnapshot("1,2", authentication).getBody();

    assertEquals("DONE", snapshot.status());
    assertThat(snapshot.result().jobsFound()).hasSize(2);
    assertEquals("https://example.com/job-2", snapshot.result().jobsFound().getFirst().url());
    assertThat(snapshot.events()).hasSize(2);
  }

  @Test
  void shouldReturnFailedSnapshotWhenAnyOrderFailed() {
    Authentication authentication = authenticatedAsUser1();
    JobOrderEntity failed = jobOrder(1L, EngineType.GROK, "grok-4-1-fast-non-reasoning", OrderStatus.FAILED);
    failed.setErrorMessage("GROK request timed out");
    JobOrderEntity completed = jobOrder(2L, EngineType.SERP, "google_jobs", OrderStatus.COMPLETED);
    when(orderProgressStore.get(1L)).thenReturn(List.of());
    when(orderProgressStore.get(2L)).thenReturn(List.of());
    when(jobOrderDBService.getJobOrders(List.of(1L, 2L))).thenReturn(List.of(failed, completed));

    SearchJobSnapshotResponse snapshot = controller.searchJobSnapshot("1,2", authentication).getBody();

    assertEquals("FAILED", snapshot.status());
    assertThat(snapshot.errorMessage()).contains("GROK request timed out");
  }

  @Test
  void shouldReturnNotFoundWhenSearchIdBelongsToAnotherUser() {
    Authentication authentication = authenticatedAsUser1();
    UserEntity otherUser = new UserEntity();
    otherUser.setId(999L);
    JobOrderEntity foreignOrder = new JobOrderEntity(otherUser,
        new AiModelEntity(EngineType.GROK, "grok-4-1-fast-non-reasoning"), false, true);
    foreignOrder.setId(1L);
    foreignOrder.setStatus(OrderStatus.COMPLETED);
    when(orderProgressStore.get(1L)).thenReturn(List.of());
    when(jobOrderDBService.getJobOrders(List.of(1L))).thenReturn(List.of(foreignOrder));

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class, () -> controller.searchJobSnapshot("1", authentication));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenSearchIdIsMalformed() {
    Authentication authentication = authenticatedAsUser1();

    ResponseStatusException ex = assertThrows(
        ResponseStatusException.class, () -> controller.searchJobSnapshot("not-a-number", authentication));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  private Authentication authenticatedAsUser1() {
    Authentication authentication = new UsernamePasswordAuthenticationToken(USERNAME, "n/a");
    UserEntity user = new UserEntity();
    user.setId(1L);
    user.setUsername(USERNAME);
    user.setEmail(USERNAME);
    user.setCv(new UserCvEntity());
    when(userDBService.getUserCompleteInfo(USERNAME)).thenReturn(Optional.of(user));
    when(userDBService.getUserByEmail(anyString())).thenReturn(Optional.empty());
    return authentication;
  }

  private JobOrderEntity jobOrder(Long id, EngineType provider, String model, OrderStatus status) {
    UserEntity user = new UserEntity();
    user.setId(1L);
    user.setUsername(USERNAME);
    JobOrderEntity order = new JobOrderEntity(user, new AiModelEntity(provider, model), false, true);
    order.setId(id);
    order.setStatus(status);
    return order;
  }

  private UserJobEntity userJob(String url, AiModelEntity aiModel, int score) {
    UserJobEntity job = new UserJobEntity(new UserEntity(), url, aiModel, null);
    job.setScore(score);
    return job;
  }

  private List<JobOrderRequest> validRequest() {
    return List.of(new JobOrderRequest(EngineType.GROK, "grok-4-1-fast-non-reasoning", false, true));
  }
}
