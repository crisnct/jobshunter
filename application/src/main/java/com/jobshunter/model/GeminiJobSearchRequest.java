package com.jobshunter.model;

import com.jobshunter.dto.AIJobSearchRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeminiJobSearchRequest extends AIJobSearchRequest {

  private String base64CV;

  public GeminiJobSearchRequest(SearchJobOrder order, String base64CV) {
    super(order);
    this.base64CV = base64CV;
  }

  @Override
  public GeminiJobSearchRequest copy() {
    GeminiJobSearchRequest copy = new GeminiJobSearchRequest(getOrder(), this.base64CV);
    // Copy parent fields
    copy.setPromptId(this.getPromptId());
    copy.setUserPrompt(this.getUserPrompt());
    return copy;
  }

}
