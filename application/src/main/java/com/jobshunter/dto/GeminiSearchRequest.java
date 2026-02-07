package com.jobshunter.dto;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.SearchJobOrder;
import lombok.Getter;

/**
 * Immutable request for the Gemini provider.
 * <p>
 * Adds {@code fileId}, {@code company}, {@code discoveryModel}, and {@code companiesModel}
 * on top of the common fields.
 */
@Getter
public final class GeminiSearchRequest implements JobSearchRequest {

  private final SearchJobOrder order;
  private final String userPrompt;
  private final Long promptId;
  private final String fileId;
  private final CompanyDto company;
  private final AiModelEntity discoveryModel;
  private final AiModelEntity companiesModel;

  private GeminiSearchRequest(Builder builder) {
    this.order = builder.order;
    this.userPrompt = builder.userPrompt;
    this.promptId = builder.promptId;
    this.fileId = builder.fileId;
    this.company = builder.company;
    this.discoveryModel = builder.discoveryModel;
    this.companiesModel = builder.companiesModel;
  }

  public static Builder builder(SearchJobOrder order) {
    return new Builder(order);
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  public static final class Builder implements JobSearchRequest.Builder {

    private SearchJobOrder order;
    private String userPrompt;
    private Long promptId;
    private String fileId;
    private CompanyDto company;
    private AiModelEntity discoveryModel;
    private AiModelEntity companiesModel;

    public Builder(SearchJobOrder order) {
      this.order = order;
    }

    private Builder(GeminiSearchRequest source) {
      this.order = source.order;
      this.userPrompt = source.userPrompt;
      this.promptId = source.promptId;
      this.fileId = source.fileId;
      this.company = source.company;
      this.discoveryModel = source.discoveryModel;
      this.companiesModel = source.companiesModel;
    }

    @Override
    public Builder userPrompt(String userPrompt) {
      this.userPrompt = userPrompt;
      return this;
    }

    public Builder promptId(Long promptId) {
      this.promptId = promptId;
      return this;
    }

    @Override
    public Builder fileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public Builder company(CompanyDto company) {
      this.company = company;
      return this;
    }

    public Builder discoveryModel(AiModelEntity discoveryModel) {
      this.discoveryModel = discoveryModel;
      return this;
    }

    public Builder companiesModel(AiModelEntity companiesModel) {
      this.companiesModel = companiesModel;
      return this;
    }

    @Override
    public GeminiSearchRequest build() {
      return new GeminiSearchRequest(this);
    }
  }
}
