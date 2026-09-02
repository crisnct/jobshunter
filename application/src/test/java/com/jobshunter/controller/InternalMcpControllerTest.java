package com.jobshunter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.service.JobOrderDBService;
import com.jobshunter.database.service.UserDBService;
import com.jobshunter.dto.JobOrderRequest;
import com.jobshunter.dto.UserInfoResponse;
import com.jobshunter.model.EngineType;
import com.jobshunter.service.application.JobOrderProcessor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class InternalMcpControllerTest {

  private UserDBService userDBService;
  private InternalMcpController controller;

  @BeforeEach
  void setUp() {
    userDBService = mock(UserDBService.class);
    JobOrderDBService jobOrderDBService = mock(JobOrderDBService.class);
    JobOrderProcessor jobOrderProcessor = mock(JobOrderProcessor.class);
    controller = new InternalMcpController(userDBService, jobOrderDBService, jobOrderProcessor);
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
    Authentication authentication = new UsernamePasswordAuthenticationToken("user@example.com", "n/a");
    UserEntity user = new UserEntity();
    user.setUsername("user@example.com");
    user.setEmail("user@example.com");
    when(userDBService.getUserCompleteInfo("user@example.com")).thenReturn(Optional.of(user));
    when(userDBService.getUserByEmail("user@example.com")).thenReturn(Optional.empty());

    UserInfoResponse response = controller.me(authentication).getBody();

    assertEquals("user@example.com", response.username());
    assertEquals("user@example.com", response.email());
  }

  private List<JobOrderRequest> validRequest() {
    return List.of(new JobOrderRequest(EngineType.GROK, "grok-4-1-fast-non-reasoning", false, true));
  }
}
