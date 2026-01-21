package com.jobshunter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.model.SearchJobOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIJobSearchRequest implements Copyable<AIJobSearchRequest> {

  private SearchJobOrder order;

  private String userPrompt;
  private Long promptId;
  private String countryIsoCode;
  private String fileId;
  private String base64CV;

  private Boolean storeConversation;
  private String prevResponseId;

  private List<String> previousURL;
  private String company;
  private AiModelEntity discoveryModel;
  private AiModelEntity companiesModel;

  public AIJobSearchRequest(SearchJobOrder order) {
    this.order = order;
  }

  @Override
  public AIJobSearchRequest copy() {
    AIJobSearchRequest copy = new AIJobSearchRequest(getOrder());
    copy.setStoreConversation(this.getStoreConversation());
    copy.setPrevResponseId(this.getPrevResponseId());
    copy.setFileId(this.getFileId());
    copy.setPreviousURL(this.getPreviousURL() != null ? new ArrayList<>(this.getPreviousURL()) : null);
    copy.userPrompt = userPrompt;
    copy.promptId = promptId;
    copy.countryIsoCode = countryIsoCode;
    copy.company = company;
    copy.base64CV = base64CV;
    copy.discoveryModel = discoveryModel;
    copy.companiesModel = companiesModel;
    return copy;
  }

}
