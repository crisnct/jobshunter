package com.jobshunter.model;

import com.jobshunter.dto.AdditionalEffortRequest;
import com.jobshunter.dto.CompanyDto;
import com.jobshunter.dto.IpInfoDetailResponse;
import com.jobshunter.dto.gptRequest.Reasoning;
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

}
