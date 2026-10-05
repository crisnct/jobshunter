package com.jobshunter.service.application.cost;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokensConsumed;
import com.jobshunter.dto.perplexityResponse.Cost;
import com.jobshunter.dto.perplexityResponse.ToolCallDetail;
import com.jobshunter.dto.perplexityResponse.Usage;
import com.jobshunter.model.EngineType;
import com.jobshunter.testsupport.AiModelFixtures;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PerplexityCostTest {

  private final DefaultCostService costService = new DefaultCostService(new ObjectMapper());
  private final AiModelEntity model = AiModelFixtures.defaultPerplexityModel();

  @Test
  void mapsUsageIncludingToolInvocationsAndReportedCost() {
    Usage usage = new Usage(30_000, 3_000, 33_000,
        Map.of("web_search", new ToolCallDetail(4, 0.01), "fetch_url", new ToolCallDetail(1, 0.0005)),
        new Cost("USD", 0.006, 0.002, 0.0105, 0.0185));

    TokensConsumed consumed = TokensConsumedMapper.fromPerplexity(usage);

    assertThat(consumed).isEqualTo(new TokensConsumed(30_000, 3_000, 5, 0.0185));
  }

  @Test
  void toleratesMissingToolDetailsAndCost() {
    Usage usage = new Usage(10, 5, 15, null, null);

    assertThat(TokensConsumedMapper.fromPerplexity(usage)).isEqualTo(new TokensConsumed(10, 5, 0, null));
    assertThat(TokensConsumedMapper.fromPerplexity(null)).isEqualTo(new TokensConsumed(0, 0, 0, null));
  }

  @Test
  void reportedCostTakesPrecedenceOverModelPrices() {
    TokensConsumed consumed = new TokensConsumed(30_000, 3_000, 5, 0.0185);

    assertThat(costService.calculatePrice(consumed, model)).isEqualTo(0.0185);
  }

  @Test
  void withoutReportedCostThePricesOfTheModelApply() {
    TokensConsumed consumed = new TokensConsumed(1_000_000, 1_000_000, 1_000_000);

    // 0.2 input + 0.75 output + 2500 tool_price per 1M calls
    assertThat(costService.calculatePrice(consumed, model)).isEqualTo(0.2 + 0.75 + 2500);
  }

  @Test
  void perplexityHasASafetyRatioForTheContextWindow() {
    assertThat(model.getProvider()).isEqualTo(EngineType.PERPLEXITY);
    assertThat(costService.getSafetyRatio(model)).isEqualTo(0.8f);
  }
}
