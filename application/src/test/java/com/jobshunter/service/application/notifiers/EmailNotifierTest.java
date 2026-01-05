package com.jobshunter.service.application.notifiers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.RoleEntity;
import com.jobshunter.database.repository.RoleRepository;
import com.jobshunter.database.service.AuthDBService;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false",
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

  @Autowired
  private RoleRepository roleRepository;

  @MockitoSpyBean
  private AuthDBService authDBService;

  @MockitoSpyBean
  private EmailNotifierService emailNotifierService;

  @MockitoSpyBean
  private SmtpMailtrapClient smtpMailtrapClient;

  @Test
  @DisplayName("Should register user via HTTP and trigger email notifier without touching DB")
  void shouldSendEmailWithFormattedJobs() throws Exception {
    RegisterRequest request = new RegisterRequest(
        "dummy.user", "test@test.com", "test1909test", "+40710221441");

    RoleEntity role = new RoleEntity();
    role.setName("USER");
    roleRepository.save(role);

    doNothing().when(smtpMailtrapClient).sendEmail(anyString(), any(), anyString());

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(authDBService).register(request);
    verify(emailNotifierService).sendVerificationToken(argThat(u ->
        u != null && request.username().equals(u.getUsername()) && request.email().equals(u.getEmail())
    ));
    verify(smtpMailtrapClient).sendEmail(eq(request.email()), any(), any());
  }

}
