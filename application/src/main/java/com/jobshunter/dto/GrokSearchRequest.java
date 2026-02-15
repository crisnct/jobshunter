package com.jobshunter.dto;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.GptSearchRequest.Builder;
import com.jobshunter.model.SearchJobOrder;
import lombok.Getter;

/**
 * Immutable request for the Grok provider.
 * <p>
 * Includes conversation fields ({@code storeConversation}, {@code prevResponseId})
 * and company-search fields.
 */
@Getter
public final class GrokSearchRequest implements JobSearchRequest {

  private final SearchJobOrder order;
  private final String userPrompt;
  private final Long promptId;
  private final String fileId;
  private final String countryIsoCode;
  private final Boolean storeConversation;
  private final String prevResponseId;
  private final CompanyDto company;
  private final AiModelEntity discoveryModel;
  private final AiModelEntity companiesModel;

  private GrokSearchRequest(Builder builder) {
    this.order = builder.order;
    this.userPrompt = builder.userPrompt;
    this.promptId = builder.promptId;
    this.fileId = builder.fileId;
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

    private SearchJobOrder order;
    private String userPrompt;
    private Long promptId;
    private String fileId;
    private String countryIsoCode;
    private Boolean storeConversation;
    private String prevResponseId;
    private CompanyDto company;
    private AiModelEntity discoveryModel;
    private AiModelEntity companiesModel;

    public Builder(SearchJobOrder order) {
      this.order = order;
    }

    private Builder(GrokSearchRequest source) {
      this.order = source.order;
      this.userPrompt = source.userPrompt;
      this.promptId = source.promptId;
      this.fileId = source.fileId;
      this.storeConversation = source.storeConversation;
      this.prevResponseId = source.prevResponseId;
      this.company = source.company;
      this.countryIsoCode = source.countryIsoCode;
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

    @Override
    public Builder fileId(String fileId) {
      this.fileId = fileId;
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
    public GrokSearchRequest build() {
      return new GrokSearchRequest(this);
    }
  }
}
