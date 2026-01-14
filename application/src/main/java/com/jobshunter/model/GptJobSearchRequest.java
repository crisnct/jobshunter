package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.gptRequest.Reasoning;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AdditionalEffortRequest {

  private final Reasoning reasoning;
  private List<CompanyDto> companies;
  private IpInfoDetailResponse ipInfo;

  public GptJobSearchRequest(SearchJobOrder order, Reasoning reasoning) {
    super(order.getUser(), order.getEngineSelection());
    this.reasoning = reasoning;
    this.ipInfo = order.getIpInfo();
  }

  protected GptJobSearchRequest(UserEntity user, EngineSelection engineSelection, Reasoning reasoning) {
    super(user, engineSelection);
    this.reasoning = reasoning;
  }

  @Override
  public GptJobSearchRequest copy() {
    GptJobSearchRequest copy = new GptJobSearchRequest(this.getUser(),
        new EngineSelection(this.getEngineSelection().type(), this.getEngineSelection().model()),
        this.reasoning);
    // Copy parent fields
    copy.setPromptId(this.getPromptId());
    copy.setUserPrompt(this.getUserPrompt());
    copy.setSearchCompanies(this.isSearchCompanies());
    copy.setStoreConversation(this.getStoreConversation());
    copy.setPrevResponseId(this.getPrevResponseId());
    copy.setFileId(this.getFileId());
    copy.setPreviousURL(this.getPreviousURL() != null ? new ArrayList<>(this.getPreviousURL()) : null);
    // Copy GptJobSearchRequest specific fields
    copy.companies = this.companies != null ? new ArrayList<>(this.companies) : null;
    copy.ipInfo = this.ipInfo;
    return copy;
  }

}
