package com.jobshunter.model;

import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AdditionalEffortRequest {

  private List<CompanyDto> companies;

  public GptJobSearchRequest(SearchJobOrder order) {
    super(order);
  }

  @Override
  public GptJobSearchRequest copy() {
    GptJobSearchRequest copy = new GptJobSearchRequest(getOrder());
    // Copy parent fields
    copy.setPromptId(this.getPromptId());
    copy.setUserPrompt(this.getUserPrompt());
    copy.setStoreConversation(this.getStoreConversation());
    copy.setPrevResponseId(this.getPrevResponseId());
    copy.setFileId(this.getFileId());
    copy.setPreviousURL(this.getPreviousURL() != null ? new ArrayList<>(this.getPreviousURL()) : null);
    // Copy GptJobSearchRequest specific fields
    copy.companies = this.companies != null ? new ArrayList<>(this.companies) : null;
    return copy;
  }

}
