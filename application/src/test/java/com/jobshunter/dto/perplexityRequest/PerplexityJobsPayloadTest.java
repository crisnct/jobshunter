package com.jobshunter.dto.perplexityRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.perplexityRequest.tools.FetchUrlTool;
import com.jobshunter.dto.perplexityRequest.tools.UserLocation;
import com.jobshunter.dto.perplexityRequest.tools.WebSearchFilters;
import com.jobshunter.dto.perplexityRequest.tools.WebSearchTool;
import com.jobshunter.model.AiCapabilityType;
import com.jobshunter.testsupport.AiModelFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class PerplexityJobsPayloadTest {

  private static final String SCHEMA = "{\"type\":\"object\",\"properties\":{\"results\":{\"type\":\"array\"}}}";

  private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

  private PerplexityJobsPayload.Builder fullBuilder(AiModelEntity model) {
    return PerplexityJobsPayload.builder(model)
        .maxOutputTokens(15000)
        .maxSteps(5)
        .store(false)
        .instructions("be precise")
        .addSystemPrompt("system text")
        .addUserPrompt("user text")
        .addTool(new WebSearchTool("low", 10, new UserLocation("RO", "Cluj-Napoca", null),
            new WebSearchFilters(List.of("-linkedin.com"), "month")))
        .setResponseSchema("job_search_results", SCHEMA);
  }

  private JsonNode serialize(PerplexityJobsPayload payload) throws Exception {
    return mapper.readTree(mapper.writeValueAsString(payload));
  }

  @Test
  void serializesPerplexityResponsesFormat() throws Exception {
    JsonNode json = serialize(fullBuilder(AiModelFixtures.defaultPerplexityModel()).previousResponseId("resp_1").build());

    assertThat(json.get("model").asText()).isEqualTo("openai/gpt-6-luna");
    assertThat(json.get("max_steps").asInt()).isEqualTo(5);
    assertThat(json.get("max_output_tokens").asInt()).isEqualTo(15000);
    assertThat(json.get("store").asBoolean()).isFalse();
    assertThat(json.get("previous_response_id").asText()).isEqualTo("resp_1");
    assertThat(json.get("instructions").asText()).isEqualTo("be precise");

    // structured output is top level response_format, not the text.format used by GPT/Grok
    assertThat(json.has("text")).isFalse();
    JsonNode format = json.get("response_format");
    assertThat(format.get("type").asText()).isEqualTo("json_schema");
    assertThat(format.get("json_schema").get("name").asText()).isEqualTo("job_search_results");
    assertThat(format.get("json_schema").get("strict").asBoolean()).isTrue();
    assertThat(format.get("json_schema").get("schema").get("type").asText()).isEqualTo("object");

    JsonNode input = json.get("input");
    assertThat(input).hasSize(2);
    assertThat(input.get(0).get("role").asText()).isEqualTo("system");
    assertThat(input.get(1).get("role").asText()).isEqualTo("user");
    assertThat(input.get(1).get("content").get(0).get("type").asText()).isEqualTo("input_text");
    assertThat(input.get(1).get("content").get(0).get("text").asText()).isEqualTo("user text");
  }

  @Test
  void serializesWebSearchToolWithPerplexityFields() throws Exception {
    JsonNode tool = serialize(fullBuilder(AiModelFixtures.defaultPerplexityModel()).build()).get("tools").get(0);

    assertThat(tool.get("type").asText()).isEqualTo("web_search");
    assertThat(tool.get("search_context_size").asText()).isEqualTo("low");
    assertThat(tool.get("max_results").asInt()).isEqualTo(10);
    // no "type: approximate" like Grok
    assertThat(tool.get("user_location").has("type")).isFalse();
    assertThat(tool.get("user_location").get("country").asText()).isEqualTo("RO");
    assertThat(tool.get("user_location").get("city").asText()).isEqualTo("Cluj-Napoca");
    assertThat(tool.get("filters").get("search_domain_filter").get(0).asText()).isEqualTo("-linkedin.com");
    assertThat(tool.get("filters").get("search_recency_filter").asText()).isEqualTo("month");
  }

  @Test
  void serializesFetchUrlTool() throws Exception {
    PerplexityJobsPayload payload = PerplexityJobsPayload.builder(AiModelFixtures.defaultPerplexityModel())
        .addTool(new FetchUrlTool(3))
        .addUserPrompt("u")
        .build();

    JsonNode tool = serialize(payload).get("tools").get(0);

    assertThat(tool.get("type").asText()).isEqualTo("fetch_url");
    assertThat(tool.get("max_urls").asInt()).isEqualTo(3);
  }

  @Test
  void omitsNullFieldsAndDefaultsStoreToFalse() throws Exception {
    JsonNode json = serialize(PerplexityJobsPayload.builder(AiModelFixtures.defaultPerplexityModel()).addUserPrompt("u").build());

    assertThat(json.has("previous_response_id")).isFalse();
    assertThat(json.has("tools")).isFalse();
    assertThat(json.has("response_format")).isFalse();
    assertThat(json.has("temperature")).isFalse();
    assertThat(json.get("store").asBoolean()).isFalse();
    // the AiModelEntity is only for cost bookkeeping and must never be sent
    assertThat(json.has("aiModel")).isFalse();
  }

  @Test
  void skipsFeaturesTheModelDoesNotDeclare() throws Exception {
    JsonNode json = serialize(fullBuilder(AiModelFixtures.perplexityModel()).build());

    assertThat(json.has("tools")).isFalse();
    assertThat(json.has("instructions")).isFalse();
    assertThat(json.has("response_format")).isFalse();
    // only the user prompt survives, there is no system prompt capability
    assertThat(json.get("input")).hasSize(1);
    assertThat(json.get("input").get(0).get("role").asText()).isEqualTo("user");
  }

  @Test
  void rejectsReasoningTogetherWithTemperature() {
    AiModelEntity model = AiModelFixtures.perplexityModel(AiCapabilityType.REASONING, AiCapabilityType.TEMPERATURE);

    PerplexityJobsPayload.Builder builder = PerplexityJobsPayload.builder(model).reasoning(new Reasoning()).temperature(0.2);

    assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsInvalidSchema() {
    PerplexityJobsPayload.Builder builder = PerplexityJobsPayload.builder(AiModelFixtures.defaultPerplexityModel());

    assertThatThrownBy(() -> builder.setResponseSchema("x", "{not json")).isInstanceOf(IllegalArgumentException.class);
  }
}
