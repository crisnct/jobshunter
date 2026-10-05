package com.jobshunter.dto;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.SearchJobOrder;
import lombok.Getter;

/**
 * Immutable request for the Perplexity provider.
 * <p>
 * Includes conversation fields ({@code storeConversation}, {@code prevResponseId}) and company-search fields. There is no {@code fileId}:
 * Perplexity has no Files API and an attached document disables its browsing tools, so the CV is never sent to discovery.
 */
@Getter
public final class PerplexitySearchRequest implements JobSearchRequest {

  private final SearchJobOrder order;
  private final String userPrompt;
  private final Long promptId;
  private final String countryIsoCode;
  private final Boolean storeConversation;
  private final String prevResponseId;
  private final CompanyDto company;
  private final AiModelEntity discoveryModel;
  private final AiModelEntity companiesModel;

  private PerplexitySearchRequest(Builder builder) {
    this.order = builder.order;
    this.userPrompt = builder.userPrompt;
    this.promptId = builder.promptId;
    this.countryIsoCode = builder.countryIsoCode;
    this.storeConversation = builder.storeConversation;
    this.prevResponseId = builder.prevResponseId;
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

  public static final class Builder implements JobSearchRequest.ConversationBuilder {

    private final SearchJobOrder order;
    private String userPrompt;
    private Long promptId;
    private String countryIsoCode;
    private Boolean storeConversation;
    private String prevResponseId;
    private CompanyDto company;
    private AiModelEntity discoveryModel;
    private AiModelEntity companiesModel;

    public Builder(SearchJobOrder order) {
      this.order = order;
    }

    private Builder(PerplexitySearchRequest source) {
      this.order = source.order;
      this.userPrompt = source.userPrompt;
      this.promptId = source.promptId;
      this.countryIsoCode = source.countryIsoCode;
      this.storeConversation = source.storeConversation;
      this.prevResponseId = source.prevResponseId;
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

    public Builder countryIsoCode(String countryIsoCode) {
      this.countryIsoCode = countryIsoCode;
      return this;
    }

    /**
     * Required by the generic builder contract and intentionally ignored: Perplexity has no file ids.
     */
    @Override
    public Builder fileId(String fileId) {
      return this;
    }

    public Builder storeConversation(Boolean storeConversation) {
      this.storeConversation = storeConversation;
      return this;
    }

    @Override
    public Builder prevResponseId(String prevResponseId) {
      this.prevResponseId = prevResponseId;
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
    public PerplexitySearchRequest build() {
      return new PerplexitySearchRequest(this);
    }
  }
}
