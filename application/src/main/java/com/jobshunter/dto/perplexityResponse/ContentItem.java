package com.jobshunter.dto.perplexityResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContentItem(String type, String text, List<Annotation> annotations) {

  public static final String TYPE_OUTPUT_TEXT = "output_text";
}
