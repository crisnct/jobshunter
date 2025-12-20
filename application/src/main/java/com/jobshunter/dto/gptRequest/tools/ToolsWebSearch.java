package com.jobshunter.dto.gptRequest.tools;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ToolsWebSearch extends Tools {

  private UserLocation userLocation;

  public ToolsWebSearch() {
    super("web_search_preview");
  }

  public ToolsWebSearch(String type) {
    super(type);
  }

}
