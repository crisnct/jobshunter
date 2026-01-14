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
public class GptJobSearchRequest extends AdditionalEffortRequest implements Cloneable {

  private final Reasoning reasoning;
  private List<CompanyDto> companies;
  private IpInfoDetailResponse ipInfo;

  public GptJobSearchRequest(UserEntity user, EngineSelection engineSelection) {
    super(user, engineSelection);
    this.reasoning = null;
  }

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
  public GptJobSearchRequest clone() {
    // Create new instance with same reasoning (final field requires constructor)
    GptJobSearchRequest clone = new GptJobSearchRequest(this.getUser(), this.getEngineSelection(), this.reasoning);

    // Copy parent fields from super.clone()
    AdditionalEffortRequest parentClone = super.clone();
    clone.setPromptId(parentClone.getPromptId());
    clone.setUserPrompt(parentClone.getUserPrompt());
    clone.setSearchCompanies(parentClone.isSearchCompanies());
    clone.setStoreConversation(parentClone.getStoreConversation());
    clone.setPrevResponseId(parentClone.getPrevResponseId());
    clone.setFileId(parentClone.getFileId());
    clone.setPreviousURL(parentClone.getPreviousURL());

    // Copy GptJobSearchRequest specific fields
    clone.companies = this.companies != null ? new ArrayList<>(this.companies) : null;
    clone.ipInfo = this.ipInfo;

    return clone;

  }

}
