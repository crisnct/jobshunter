package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.SearchJobOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Immutable request object for AI job search operations.
 * Use {@link #builder(SearchJobOrder)} to create new instances.
 * Use {@link #toBuilder()} to create modified copies.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AIJobSearchRequest {

  private final SearchJobOrder order;
  private final String userPrompt;
  private final Long promptId;
  private final String countryIsoCode;
  private final String fileId;
  private final String base64CV;
  private final Boolean storeConversation;
  private final String prevResponseId;
  private final List<String> previousURL;
  private final CompanyDto company;
  private final AiModelEntity discoveryModel;
  private final AiModelEntity companiesModel;

  private AIJobSearchRequest(Builder builder) {
    this.order = builder.order;
    this.userPrompt = builder.userPrompt;
    this.promptId = builder.promptId;
    this.countryIsoCode = builder.countryIsoCode;
    this.fileId = builder.fileId;
    this.base64CV = builder.base64CV;
    this.storeConversation = builder.storeConversation;
    this.prevResponseId = builder.prevResponseId;
    this.previousURL = builder.previousURL != null ? List.copyOf(builder.previousURL) : null;
    this.company = builder.company;
    this.discoveryModel = builder.discoveryModel;
    this.companiesModel = builder.companiesModel;
  }

  /**
   * Creates a new builder with the required order parameter.
   */
  public static Builder builder(SearchJobOrder order) {
    return new Builder(order);
  }

  /**
   * Creates a builder pre-populated with this instance's values.
   * Use this to create modified copies of the request.
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Builder for creating AIJobSearchRequest instances.
   */
  public static final class Builder {

    private SearchJobOrder order;
    private String userPrompt;
    private Long promptId;
    private String countryIsoCode;
    private String fileId;
    private String base64CV;
    private Boolean storeConversation;
    private String prevResponseId;
    private List<String> previousURL;
    private CompanyDto company;
    private AiModelEntity discoveryModel;
    private AiModelEntity companiesModel;

    /**
     * Creates a new builder with the required order.
     */
    public Builder(SearchJobOrder order) {
      this.order = order;
    }

    /**
     * Copy constructor - creates a builder from an existing request.
     */
    private Builder(AIJobSearchRequest source) {
      this.order = source.order;
      this.userPrompt = source.userPrompt;
      this.promptId = source.promptId;
      this.countryIsoCode = source.countryIsoCode;
      this.fileId = source.fileId;
      this.base64CV = source.base64CV;
      this.storeConversation = source.storeConversation;
      this.prevResponseId = source.prevResponseId;
      this.previousURL = source.previousURL != null ? new ArrayList<>(source.previousURL) : null;
      this.company = source.company;
      this.discoveryModel = source.discoveryModel;
      this.companiesModel = source.companiesModel;
    }

    public Builder order(SearchJobOrder order) {
      this.order = order;
      return this;
    }

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

    public Builder fileId(String fileId) {
      this.fileId = fileId;
      return this;
    }

    public Builder base64CV(String base64CV) {
      this.base64CV = base64CV;
      return this;
    }

    public Builder storeConversation(Boolean storeConversation) {
      this.storeConversation = storeConversation;
      return this;
    }

    public Builder prevResponseId(String prevResponseId) {
      this.prevResponseId = prevResponseId;
      return this;
    }

    public Builder previousURL(List<String> previousURL) {
      this.previousURL = previousURL;
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

    /**
     * Builds the immutable AIJobSearchRequest instance.
     */
    public AIJobSearchRequest build() {
      return new AIJobSearchRequest(this);
    }
  }
}
