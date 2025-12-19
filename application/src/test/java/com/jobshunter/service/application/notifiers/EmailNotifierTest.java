package com.jobshunter.service.application.notifiers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.repository.UserRepository;
import com.jobshunter.database.service.AuthService;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration",
        "jobshunter.useDummyData=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailNotifierTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper mapper;

  //MOCKS -------------------------------
  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private RoleRepository roleRepository;
  //--------------------------------------

  @MockitoSpyBean
  private AuthService authService;

  @MockitoSpyBean
  private EmailNotifierService emailNotifierService;

  @MockitoSpyBean
  private SmtpMailtrapClient smtpMailtrapClient;

  @Test
  @DisplayName("Should register user via HTTP and trigger email notifier without touching DB")
  void shouldSendEmailWithFormattedJobs() throws Exception {
    RegisterRequest request = new RegisterRequest(
        "dummy.user", "test@test.com", "test1909test", "+40710221441");

    UserEntity user = new UserEntity();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setVerificationToken("token-123");

    RoleEntity testRole = new RoleEntity();
    testRole.setId(1L);
    testRole.setName("TEST");

    when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
    when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(false);
    when(userRepository.existsByPhoneNumberIgnoreCase(any())).thenReturn(false);
    when(userRepository.save(any())).thenReturn(user);
    when(roleRepository.findByName(any())).thenReturn(Optional.of(testRole));
    doNothing().when(smtpMailtrapClient).sendEmail(anyString(), any(), anyString());

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(authService).register(request);
    verify(emailNotifierService).sendVerificationToken(user);
    verify(smtpMailtrapClient).sendEmail(eq(request.email()), any(), any());
  }

}
