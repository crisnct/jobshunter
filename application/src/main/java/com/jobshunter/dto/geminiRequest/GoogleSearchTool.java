package com.jobshunter.dto.geminiRequest;

import lombok.Data;

@Data
public class GoogleSearchTool implements Tool {

  private GoogleSearch google_search = new GoogleSearch();

  public record GoogleSearch() {

  }
}
