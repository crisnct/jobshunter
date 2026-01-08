package com.jobshunter.service.application;

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
import com.jobshunter.service.UrlAffinityExecutor;
import com.jobshunter.service.application.notifiers.EmailNotifierService;
import com.jobshunter.service.clients.SmtpMailtrapClient;
import com.jobshunter.service.clients.browser.BrowserSimulator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationTests {

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

  @Autowired
  private BrowserSimulator browserSimulator;

  @Autowired
  private UrlAffinityExecutor executor;

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

  @Test
  @Disabled
  public void testSameHosts() {
    for (int i = 0; i < 2; i++) {
      long startTime = System.currentTimeMillis();
      List<CompletionStage<ResponseEntity<String>>> list = new ArrayList<>();
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4325917621/?alternateChannel=search&eBP=CwEAAAGbmZoR96OgJKqJKiN2eauDc9WvxIjFZZziR3Xlrj7t2rajIc8hfS6W5si9d_tFs7awpLKIfiDovN-FBOLzaTdB0Tujt16TQEHK69v4N5ZqLHEUU5YoCgFTE0fPXj26OqbyBjgidHfFSfzYj0cJ5PpIaJlGvnBWvZMcuM4w7bXBIwjHqvy-bqBS5O9jJtZPNNjdgbHafEilueURNWa8QYHVFqIWnnG27VcGqMik9UO6L-uZ6ppEYrNlE43Nb6oVQRQYdm0_NeS42McELXfYBSdG2ZFd6InuJ_RN7_TNtKvsmEJCdTUbWv3kL7yzIz0n0G7ErcMB1aGRLc3OCXyghJlnrSM8wdpOlc6Rg-30g5ZbKaMubgPR3GOvZXcar_r7C4BD687Ty8i4eqUssb-5jIjWMWwIuZpwxjpHqrxjEBDQu0V_BkeQD7ahWnGxDv-DrFPVIZ4RrLO_C3broycF1rTk5dMrg5TUGJ0XQdyzmD40bwDpsQDhK9ngt8Le_xng27GXOQ&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=DUAyEyVFaOVKf%2FU0NjDJFw%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4340490377/?alternateChannel=search&eBP=CwEAAAGbmZoR9vJgMa2RnK6BJQ3zLQpl7RFATF4Pfz7cYeX75SHOzEzTPTOZISBoZC0h0OfzxnsPei126MJJUL1_gqJhv6wHXDYvSdc2QL_xfa0MLAUzRDYQDIbZ71PiEVmAL-UwhI3UNKIzS7Y8JCenKYbKgMC-tGm_oZHIQs1PYqvsoU9TR9S0T6WqqXYdfYDpwap8QEIC4d4Hz7lffJSZ6xgHAE1A-gGDriCZlG-6jmCtEPNedgN5a-mrTfVfjlTkCQwQ5cdSNN85a_DfMn4xYoAk4MvP7p-_9rOkM_59DsX-r-4NdNNLdvHkKcQPc2kqRk8GXENGMYaOhTp6MqMZf2maVJuQ2S-faaXZJtmCl51yOmTBBz1NstPVH7cYp1sYbwWghqaydUJdWPid0l9p5qze2bAgILFZxnRgybn3V5nG206GrzYuDiQmdwb0Wt7RBUvmne2c5ZuVHnU3QMWQEeNgtgJ4iXwwehZnnKEa_piVyoqMvCrQK_09LEhFNKc_cjg&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=aiIgWGFG98TStDzAZk5%2Bkw%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4343782499/?alternateChannel=search&eBP=CwEAAAGbmZoR9rGfc81XoFIhWSGS3zqXhJFF9FUeXIfoEhpryREexluI1WJYbOHxre6Dc_yeC6Kd4lKlhY4ulxQT74w-pkPpmRGF66Rl7Cz5aC9ldvmzGPGCcKNVmxSijfmOzUvGHK7LQGdHc6DufEvEweFJgMjsygMF34lZw82qj2eOOdswKGY1fY79mQXxFFW3f1eaQ-f3EJ4678BkBN7i-FP9bJ7Ei0X4TG9vh_ROFfvJ4QUHeeliN6aSXrjLxtZm1eP2d01B1Jp8ZJbo_G590Aa81G3ln9dap3VxIvYzmeRNdWMt16YRI-rWgwABDS7SrTYrNcinqaNMXy5GVj7EdBn4rBKruet-tjt6rHYiHlrUTxrztfuYsYEOVzHpHrk3nzalhXOdmQ-T2JdqsjFIw7ZrQQkREh5TiVvQ3TzSbm_ZWysBxeN8HfoB-yh9aoZy_orWDq2uyueIAPtuekOytX3SGj0JVyK2-qm3X6suDYpv9BPqiPbod5ElaWDDA7SkaXY&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=%2FZ%2Fyqahiann9VsRJ%2FKPmxQ%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4329095503/?alternateChannel=search&eBP=CwEAAAGbmZoR99qBig-C1wt2WPgh7Frna-t96W8aNBitkn2D-utKuA2IktG4qi1HduV20yddVqqDR3jxuyub6r_0TeF0YBXstj4M3TpBg6DxKDiqnU-VTbVtfMDW3T4ZL5BP1AIvHuvo3C7JzO-JSn0H2seiq1UQ_rfNMssPTox_utyWU_Hap8g0bSkmjEsO9K45z_VOlsLfb2i6w3pQoeVAc0Tz6fdxtOL1aKZ9K-fIhXsMx9Qz-mYc5W9YrnwcKWopTst83fpuhhtAvL0X8NzNfvtBjHgEfcUaH-wRdS2oOwua3ICmTwgIYXhiQYTA8wj175VG_Ez0qujIZ03xpWRTq8oe8BoLw7TNKzrsierf6fGlAeM3BSsiLkIVNuyU1JK0NCo8qYq33CdmicAKFdFQULL6A3yCmYtyLERKQFabGxSKy-j7bu-15-K9OqxciCTHSCUUIyFIpJIuShRRjMdj7uTlqVJXLacjmCWTsEsuf-hWk-AeKvGZnTQsZC07aCRozWmyGA&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=DXt5abDM0KhloVrKQBuECw%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4327883662/?alternateChannel=search&eBP=CwEAAAGbmZoR94KcJfv5souH4VlB4kdaW7a9wbZOkyWH0OKKZ1_ga5TN_XZLFYR8DmnOOJU07RxZqaPA8aHP4VQZas3qWjh6U22HRkxR5vXHFCBlz8xXF70JhPgIDVxgjwOVJJEQcJVLdscdk7-w0y-XCUcx5dK6uSnvNcd-AYNNi-L7VjR2ENtb8Q7xw4lZ-YKQIFYB7zpVWbApmduQ9oZMOKfnzZko2MT33aKK1PbZNNkNCPyEsy4RZ2ZKYWJOLl4gAhJ2QKeG8Gre9aUY1OjKJR9cJRSpx5rrNZyd-aC2VImrP48LIbrsSU0lkjZklNXzQxwSCYz1sQmadvdUY4T1CmiTFOdrL1ZApoV4awJzTQ9KAuM_U7gzRLRWhDtMsOBOmbRDvlQaVcOuN1c9NBBUGT8_u3jbN_iRlEgUHiZlCSYcj6H79xV9cocELSs_7z5L2aSag777sBKcxrD2WM2NPw0yZY6ArXOPGzFVnJeriFnCoZMMs9ra8pgUiKRf1znLHzc&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=u2hGmJoGASlk4MUIjjjwbA%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/it-team-lead-active-directory-5?rid=bc93c49b-353e-4140-8647-23935a158dbc&pos=22&selectedJobSlug=it-team-lead-active-directory-5"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/head-of-hr-communication-applications?rid=bc93c49b-353e-4140-8647-23935a158dbc&pos=21&selectedJobSlug=head-of-hr-communication-applications"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/global-it-team-lead-sap-cross-applications-m-f?rid=94772615-7eca-49f6-bb36-6eb3298ccbee&pos=2&selectedJobSlug=global-it-team-lead-sap-cross-applications-m-f"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/network-security-engineer-in-ba-oesl?rid=bc93c49b-353e-4140-8647-23935a158dbc&pos=38&selectedJobSlug=network-security-engineer-in-ba-oesl"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/telecom-engineer-timisoara?rid=743c2183-0e7e-4595-b985-49f19391b84d&pos=47&selectedJobSlug=telecom-engineer-timisoara"));
      list.forEach(l -> l.toCompletableFuture().join());
      logStatus(startTime);
    }
  }

  @Test
  @Disabled
  public void testDifferentHosts() {
    for (int i = 0; i < 1; i++) {
      long startTime = System.currentTimeMillis();
      List<CompletionStage<ResponseEntity<String>>> list = new ArrayList<>();
      list.add(browserSimulator.openPageAsync(
          "https://www.linkedin.com/jobs/view/4340490377/?alternateChannel=search&eBP=CwEAAAGbmZoR9vJgMa2RnK6BJQ3zLQpl7RFATF4Pfz7cYeX75SHOzEzTPTOZISBoZC0h0OfzxnsPei126MJJUL1_gqJhv6wHXDYvSdc2QL_xfa0MLAUzRDYQDIbZ71PiEVmAL-UwhI3UNKIzS7Y8JCenKYbKgMC-tGm_oZHIQs1PYqvsoU9TR9S0T6WqqXYdfYDpwap8QEIC4d4Hz7lffJSZ6xgHAE1A-gGDriCZlG-6jmCtEPNedgN5a-mrTfVfjlTkCQwQ5cdSNN85a_DfMn4xYoAk4MvP7p-_9rOkM_59DsX-r-4NdNNLdvHkKcQPc2kqRk8GXENGMYaOhTp6MqMZf2maVJuQ2S-faaXZJtmCl51yOmTBBz1NstPVH7cYp1sYbwWghqaydUJdWPid0l9p5qze2bAgILFZxnRgybn3V5nG206GrzYuDiQmdwb0Wt7RBUvmne2c5ZuVHnU3QMWQEeNgtgJ4iXwwehZnnKEa_piVyoqMvCrQK_09LEhFNKc_cjg&refId=7WEPxO81TSXPY6FEDKzhEA%3D%3D&trackingId=aiIgWGFG98TStDzAZk5%2Bkw%3D%3D&trk=d_flagship3_search_srp_jobs"));
      list.add(browserSimulator.openPageAsync(
          "https://www.bestjobs.eu/loc-de-munca/telecom-engineer-timisoara?rid=743c2183-0e7e-4595-b985-49f19391b84d&pos=47&selectedJobSlug=telecom-engineer-timisoara"));
      list.add(browserSimulator.openPageAsync("https://www.techtalent.ro/careers/java-developer/"));
      list.add(browserSimulator.openPageAsync("https://wellfound.com/jobs/3709274-senior-java-backend-developer"));
      list.add(
          browserSimulator.openPageAsync("https://www.accenture.com/ro-en/careers/jobdetails?id=R00300960_en&title=PharmaSuite+MES+Workstream+Lead"));
      list.add(browserSimulator.openPageAsync("https://devjob.ro/en/jobs/Showpad-Senior-Data-Platform--Cloud-Engineer"));
      list.add(browserSimulator.openPageAsync("https://careers.google.com/jobs/results/"));
      list.add(browserSimulator.openPageAsync("https://www.amazon.jobs/en/"));
      list.add(browserSimulator.openPageAsync("https://jobs.apple.com/en-us/search?location=united-states-USA"));
      list.add(browserSimulator.openPageAsync("https://careers.microsoft.com/v2/global/en/search"));
      list.add(browserSimulator.openPageAsync("https://www.oracle.com/corporate/careers/"));
      list.add(browserSimulator.openPageAsync("https://jobs.sap.com/"));
      list.add(browserSimulator.openPageAsync("https://www.redhat.com/en/jobs"));
      list.add(browserSimulator.openPageAsync("https://www.lifeatspotify.com/jobs/gm-surfaces-personalization"));
      list.add(browserSimulator.openPageAsync("https://jobs.bytedance.com/en/"));
      list.add(browserSimulator.openPageAsync("https://careers.booking.com/"));
      list.add(browserSimulator.openPageAsync("https://jobs.lever.co/"));
      list.add(browserSimulator.openPageAsync("https://boards.greenhouse.io/"));
      list.add(browserSimulator.openPageAsync("https://jobs.workable.com/"));
      list.add(browserSimulator.openPageAsync("https://jobs.smartrecruiters.com/"));
      list.add(browserSimulator.openPageAsync("https://www.glassdoor.com/Job/index.htm"));
      list.add(browserSimulator.openPageAsync("https://www.indeed.com/jobs"));
      list.add(browserSimulator.openPageAsync("https://www.monster.com/jobs/"));
      list.add(browserSimulator.openPageAsync("https://www.simplyhired.com/"));
      list.add(browserSimulator.openPageAsync("https://remote.com/jobs"));
      list.add(browserSimulator.openPageAsync("https://otta.com/jobs"));
      list.add(browserSimulator.openPageAsync("https://startup.jobs/"));
      list.add(browserSimulator.openPageAsync("https://angel.co/jobs"));
      list.add(browserSimulator.openPageAsync("https://www.totaljobs.com/jobs"));
      list.add(browserSimulator.openPageAsync("https://hired.com/jobs"));
      list.add(
          browserSimulator.openPageAsync("https://apply.careers.microsoft.com/careers?query=java&start=0&pid=1970393556623182&sort_by=relevance"));

      list.forEach(l -> l.toCompletableFuture().join());
      log.info("LOG STATUS {}", i);
      logStatus(startTime);
    }
  }

  @Test
  @Disabled
  public void testPlaywright() {
    List<CompletionStage<ResponseEntity<String>>> list = new ArrayList<>();
    list.add(browserSimulator.openPageAsync("https://www.techtalent.ro/careers/java-developer/"));
    list.add(browserSimulator.openPageAsync("https://devjob.ro/en/jobs/Showpad-Senior-Data-Platform--Cloud-Engineer"));
    list.add(browserSimulator.openPageAsync("https://careers.google.com/jobs/results/"));
    list.add(browserSimulator.openPageAsync("https://www.amazon.jobs/en/"));
    list.add(browserSimulator.openPageAsync("https://www.dice.com/job-detail/ae2a6fe3-0215-4fa6-a9ee-2c2245a01ef8"));
    list.add(browserSimulator.openPageAsync("https://remote.com/jobs"));
    list.add(browserSimulator.openPageAsync("https://otta.com/jobs"));
    list.add(browserSimulator.openPageAsync("https://startup.jobs/"));
    list.add(browserSimulator.openPageAsync("https://angel.co/jobs"));
    list.add(browserSimulator.openPageAsync("https://www.totaljobs.com/jobs"));
    list.add(browserSimulator.openPageAsync("https://hired.com/jobs"));
    list.add(
        browserSimulator.openPageAsync("https://apply.careers.microsoft.com/careers?query=java&start=0&pid=1970393556623182&sort_by=relevance"));
    list.forEach(l -> l.toCompletableFuture().join());
  }

  private void logStatus(long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    log.info("Executors: {}, Duration: {}ms", executor.getAllExecutors().size(), duration);
  }
}
