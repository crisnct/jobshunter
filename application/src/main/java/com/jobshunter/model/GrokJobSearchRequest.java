package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GrokJobSearchRequest extends AdditionalEffortRequest {

  private List<CompanyDto> companies;

  public GrokJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
  }

  @Override
  public GrokJobSearchRequest copy() {
    GrokJobSearchRequest copy = new GrokJobSearchRequest(this.getUser(),
        new EngineSelection(this.getEngineSelection().type(), this.getEngineSelection().model()));
    // Copy parent fields
    copy.setPromptId(this.getPromptId());
    copy.setUserPrompt(this.getUserPrompt());
    copy.setSearchCompanies(this.isSearchCompanies());
    copy.setStoreConversation(this.getStoreConversation());
    copy.setPrevResponseId(this.getPrevResponseId());
    copy.setFileId(this.getFileId());
    copy.setPreviousURL(this.getPreviousURL() != null ? new ArrayList<>(this.getPreviousURL()) : null);
    // Copy GrokJobSearchRequest specific fields
    copy.companies = this.companies != null ? new ArrayList<>(this.companies) : null;
    return copy;
  }

}
