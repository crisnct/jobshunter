package com.jobshunter.service.clients.perplexity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.jobshunter.config.ApplicationProperties;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.JobOrderEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.database.entities.UserJobRoleEntity;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.PerplexitySearchRequest;
import com.jobshunter.model.AiClientResponse;
import com.jobshunter.model.Job;
import com.jobshunter.model.SearchJobOrder;
import com.jobshunter.model.UrlVerificationMode;
import com.jobshunter.service.TemplateRenderer;
import com.jobshunter.service.application.UrlExtractor;
import com.jobshunter.service.application.cost.AiCostPublisher;
import com.jobshunter.service.application.cost.AiRequestCostEvent;
import com.jobshunter.service.application.cost.DefaultCostService;
import com.jobshunter.service.application.cost.TokenEstimationGuard;
import com.jobshunter.service.retry.RetryTemplate;
import com.jobshunter.testsupport.AiModelFixtures;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class PerplexityV1JobSearchImplTest {

  private static final String PATH = "/v1/responses";

  private static WireMockServer server;

  private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
  private final ApplicationProperties properties = new ApplicationProperties();
  private final AiModelEntity model = AiModelFixtures.defaultPerplexityModel();

  private PerplexityV1JobSearchImpl client;

  @BeforeAll
  static void startServer() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
  }

  @AfterAll
  static void stopServer() {
    server.stop();
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    server.resetAll();
    properties.getPerplexity().setApiKey("test-key");
    properties.getPerplexity().setBaseUrl(URI.create(server.baseUrl() + PATH));
    properties.getPerplexity().setUrlVerification(UrlVerificationMode.HOST);
    properties.getJobsHunter().setBlacklist("linkedin.com, weworkremotely.com");

    // single attempt: retry policies are covered by RetryTemplateTest and would only slow these tests down
    RetryTemplate retryTemplate = mock(RetryTemplate.class);
    when(retryTemplate.execute(any(), any(), any())).thenAnswer(inv -> ((Supplier<Object>) inv.getArgument(2)).get());

    client = new PerplexityV1JobSearchImpl(
        properties,
        RestClient.create(),
        retryTemplate,
        JsonMapper.builder().findAndAddModules().build(),
        new UrlExtractor(),
        new TemplateRenderer(),
        new TokenEstimationGuard(new DefaultCostService(new ObjectMapper())),
        new AiCostPublisher(eventPublisher),
        new PerplexityUrlVerifier(properties, new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
    );
  }

  // ---------------------------------------------------------------------------
  // search by prompt
  // ---------------------------------------------------------------------------

  @Test
  void searchJobsSendsAgentPayloadAndFiltersInventedUrls() throws IOException {
    stubResponse("search_response.json");

    AiClientResponse response = client.searchJobs(request(null));

    server.verify(postRequestedFor(urlEqualTo(PATH))
        .withHeader("Authorization", equalTo("Bearer test-key"))
        .withRequestBody(matchingJsonPath("$.model", equalTo("openai/gpt-6-luna")))
        .withRequestBody(matchingJsonPath("$.max_steps", equalTo("5")))
        .withRequestBody(matchingJsonPath("$.store", equalTo("false")))
        .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_schema")))
        .withRequestBody(matchingJsonPath("$.response_format.json_schema.strict", equalTo("true")))
        .withRequestBody(matchingJsonPath("$.tools[0].type", equalTo("web_search")))
        .withRequestBody(matchingJsonPath("$.tools[0].search_context_size", equalTo("low")))
        .withRequestBody(matchingJsonPath("$.tools[0].max_results", equalTo("10")))
        .withRequestBody(matchingJsonPath("$.tools[0].user_location.country", equalTo("RO")))
        .withRequestBody(matchingJsonPath("$.tools[0].user_location.city", equalTo("Cluj-Napoca")))
        .withRequestBody(matchingJsonPath("$.tools[0].filters.search_domain_filter[0]", equalTo("-linkedin.com")))
        .withRequestBody(matchingJsonPath("$.tools[0].filters.search_domain_filter[1]", equalTo("-weworkremotely.com")))
        .withRequestBody(matchingJsonPath("$.tools[0].filters.search_recency_filter", equalTo("month"))));

    assertThat(response.getId()).isEqualTo("resp_abc");
    // the third url was invented by the model and never appeared in search_results
    assertThat(response.getJobs()).extracting(Job::getUrl)
        .containsExactly("https://boards.greenhouse.io/acme/jobs/123", "https://jobs.lever.co/globex/456");
  }

  @Test
  void searchJobsDoesNotSendPreviousResponseIdOnTheFirstRound() throws IOException {
    stubResponse("search_response.json");

    client.searchJobs(request(null));

    server.verify(postRequestedFor(urlEqualTo(PATH)).withRequestBody(matchingJsonPath("$[?(!@.previous_response_id)]")));
  }

  @Test
  void retryRoundContinuesTheConversationWithPreviousResponseId() throws IOException {
    stubResponse("search_response.json");

    client.searchJobs(request("resp_previous"));

    server.verify(postRequestedFor(urlEqualTo(PATH))
        .withRequestBody(matchingJsonPath("$.previous_response_id", equalTo("resp_previous")))
        // store=false: the response is hidden from GET but still usable as previous_response_id
        .withRequestBody(matchingJsonPath("$.store", equalTo("false"))));
  }

  @Test
  void publishesTheExactCostReportedByPerplexity() throws IOException {
    stubResponse("search_response.json");

    client.searchJobs(request(null));

    ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    var consumed = ((AiRequestCostEvent) event.getValue()).getTokensConsumed();
    assertThat(consumed.inputTokens()).isEqualTo(30000);
    assertThat(consumed.outputTokens()).isEqualTo(3000);
    assertThat(consumed.toolCalls()).isEqualTo(5);
    assertThat(consumed.reportedCostUsd()).isEqualTo(0.0185);
  }

  @Test
  void failedResponseThrowsButStillBooksTheCost() throws IOException {
    stubResponse("failed_response.json");

    assertThatThrownBy(() -> client.searchJobs(request(null)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("agent loop crashed");

    verify(eventPublisher).publishEvent(any(AiRequestCostEvent.class));
  }

  @Test
  void incompleteResponseIsParsedPartially() throws IOException {
    stubResponse("incomplete_response.json");

    AiClientResponse response = client.searchJobs(request(null));

    // truncated json falls back to url extraction, there is no evidence in the response so nothing is filtered out
    assertThat(response.getJobs()).extracting(Job::getUrl).containsExactly("https://boards.greenhouse.io/acme/jobs/123");
  }

  @Test
  void rateLimitIsSurfacedWithItsRetryAfterHeader() {
    server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(429).withHeader("Retry-After", "1")));

    assertThatThrownBy(() -> client.searchJobs(request(null)))
        .isInstanceOfSatisfying(HttpClientErrorException.TooManyRequests.class,
            e -> assertThat(e.getResponseHeaders().getFirst("Retry-After")).isEqualTo("1"));
  }

  @Test
  void emptyOutputGivesNoJobs() {
    server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":\"resp_empty\",\"status\":\"completed\",\"output\":[]}")));

    AiClientResponse response = client.searchJobs(request(null));

    assertThat(response.getJobs()).isEmpty();
    assertThat(response.getId()).isEqualTo("resp_empty");
  }

  // ---------------------------------------------------------------------------
  // companies
  // ---------------------------------------------------------------------------

  @Test
  void searchCompaniesUsesShorterAgentLoopAndNoDomainFilter() throws IOException {
    stubResponse("companies_response.json");

    List<CompanyDto> companies = client.searchCompanies(request(null));

    server.verify(postRequestedFor(urlEqualTo(PATH))
        .withRequestBody(matchingJsonPath("$.max_steps", equalTo("3")))
        .withRequestBody(matchingJsonPath("$.tools[0].type", equalTo("web_search")))
        .withRequestBody(matchingJsonPath("$[?(!@.tools[0].filters)]"))
        .withRequestBody(matchingJsonPath("$.response_format.json_schema.name", equalTo("companies_results"))));
    assertThat(companies).containsExactly(new CompanyDto("Acme", "https://careers.acme.com"));
  }

  @Test
  void searchJobsFromCompaniesRestrictsTheSearchToTheCompanyAndFetchesItsCareersPage() throws IOException {
    stubResponse("search_response.json");
    PerplexitySearchRequest request = requestBuilder(null)
        .company(new CompanyDto("Acme", "https://www.careers.acme.com/jobs"))
        .build();

    client.searchJobsFromCompanies(request);

    server.verify(postRequestedFor(urlEqualTo(PATH))
        .withRequestBody(matchingJsonPath("$.tools[0].type", equalTo("web_search")))
        // allowlist: careers host first (without www), then the ATS domains, never mixed with the "-" denylist
        .withRequestBody(matchingJsonPath("$.tools[0].filters.search_domain_filter[0]", equalTo("careers.acme.com")))
        .withRequestBody(matchingJsonPath("$.tools[0].filters.search_domain_filter[1]", equalTo("greenhouse.io")))
        .withRequestBody(matchingJsonPath("$[?(!@.tools[0].filters.search_recency_filter)]"))
        .withRequestBody(matchingJsonPath("$.tools[1].type", equalTo("fetch_url")))
        .withRequestBody(matchingJsonPath("$.tools[1].max_urls", equalTo("3"))));
  }

  // ---------------------------------------------------------------------------
  // response parsing and request helpers
  // ---------------------------------------------------------------------------

  @Test
  void invalidJsonFallsBackToUrlExtraction() {
    var item = new com.jobshunter.dto.perplexityResponse.OutputItem(null, "message", null,
        List.of(new com.jobshunter.dto.perplexityResponse.ContentItem("output_text",
            "Here you go: https://jobs.lever.co/acme/1 and https://jobs.lever.co/acme/2", null)),
        null, null, null);
    var response = new com.jobshunter.dto.perplexityResponse.PerplexityResponse("r", "completed", "m", List.of(item), null, null);

    assertThat(client.extractJobs(response)).extracting(Job::getUrl)
        .containsExactly("https://jobs.lever.co/acme/1", "https://jobs.lever.co/acme/2");
  }

  @Test
  void denylistPrefixesDomainsAndCapsAtTwentyEntries() {
    List<String> domains = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      domains.add("site" + i + ".com");
    }

    List<String> filter = PerplexityV1JobSearchImpl.denylist(String.join(", ", domains));

    assertThat(filter).hasSize(20).allMatch(d -> d.startsWith("-"));
    assertThat(filter.get(0)).isEqualTo("-site0.com");
    assertThat(filter.get(19)).isEqualTo("-site19.com");
  }

  @Test
  void denylistIsAbsentForABlankBlacklist() {
    assertThat(PerplexityV1JobSearchImpl.denylist(null)).isNull();
    assertThat(PerplexityV1JobSearchImpl.denylist(" , ")).isNull();
  }

  @Test
  void companyAllowlistStartsWithTheCareersHostAndStaysWithinTheApiLimit() {
    List<String> allowlist = PerplexityV1JobSearchImpl.companyAllowlist("https://www.careers.acme.com/jobs");

    assertThat(allowlist.get(0)).isEqualTo("careers.acme.com");
    assertThat(allowlist).containsAll(PerplexityV1JobSearchImpl.ATS_DOMAINS).hasSizeLessThanOrEqualTo(20);
    assertThat(PerplexityV1JobSearchImpl.companyAllowlist("not a url")).isEqualTo(PerplexityV1JobSearchImpl.ATS_DOMAINS);
  }

  // ---------------------------------------------------------------------------
  // helpers
  // ---------------------------------------------------------------------------

  private PerplexitySearchRequest request(String previousResponseId) {
    return requestBuilder(previousResponseId).build();
  }

  private PerplexitySearchRequest.Builder requestBuilder(String previousResponseId) {
    UserEntity user = new UserEntity();
    user.setUsername("tester");
    user.setCity("Cluj-Napoca");
    user.setCountry("Romania");
    user.setJobDomain("IT");
    user.setJobRoles(List.of(new UserJobRoleEntity(user, "Java Developer")));
    JobOrderEntity jobOrder = new JobOrderEntity(user, model, true, true);
    SearchJobOrder order = new SearchJobOrder(jobOrder, user, List.of());

    PerplexitySearchRequest.Builder builder = PerplexitySearchRequest.builder(order)
        .countryIsoCode("RO")
        .storeConversation(false)
        .userPrompt("Find senior Java jobs")
        .discoveryModel(model)
        .companiesModel(model);
    return previousResponseId == null ? builder : builder.prevResponseId(previousResponseId);
  }

  private void stubResponse(String fixture) throws IOException {
    String body;
    try (var in = getClass().getResourceAsStream("/perplexity/" + fixture)) {
      assertThat(in).as("fixture " + fixture).isNotNull();
      body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(body)));
  }
}
