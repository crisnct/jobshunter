package com.jobshunter.dto;

import com.jobshunter.model.SearchJobOrder;
import lombok.Getter;

/**
 * Immutable request for the SERP provider.
 * <p>
 * SERP only needs the common fields ({@code order}, {@code userPrompt}, {@code promptId}).
 */
@Getter
public final class SerpSearchRequest implements JobSearchRequest {

  private final SearchJobOrder order;
  private final String userPrompt;
  private final Long promptId;

  private SerpSearchRequest(Builder builder) {
    this.order = builder.order;
    this.userPrompt = builder.userPrompt;
    this.promptId = builder.promptId;
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

    public Builder(SearchJobOrder order) {
      this.order = order;
    }

    private Builder(SerpSearchRequest source) {
      this.order = source.order;
      this.userPrompt = source.userPrompt;
      this.promptId = source.promptId;
    }

    @Override
    public Builder userPrompt(String userPrompt) {
      this.userPrompt = userPrompt;
      return this;
    }

    /** No-op for SERP -- included to satisfy the common {@link JobSearchRequest.Builder} contract. */
    @Override
    public Builder fileId(String fileId) {
      return this;
    }

    @Override
    public SerpSearchRequest build() {
      return new SerpSearchRequest(this);
    }
  }
}
