package com.jobshunter.model;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.database.entities.UserEntity;
import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.IpInfoDetailResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GptJobSearchRequest extends AdditionalEffortRequest {

  private List<CompanyDto> companies;
  private IpInfoDetailResponse ipInfo;

  public GptJobSearchRequest(SearchJobOrder order) {
    super(order.getUser(), order.getModel());
    this.ipInfo = order.getIpInfo();
  }

  protected GptJobSearchRequest(UserEntity user, AiModelEntity model) {
    super(user, model);
  }

  @Override
  public GptJobSearchRequest copy() {
    GptJobSearchRequest copy = new GptJobSearchRequest(this.getUser(), getModel());
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
