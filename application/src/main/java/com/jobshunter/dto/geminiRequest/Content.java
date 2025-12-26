package com.jobshunter.dto.geminiRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record Content(
    String role,
    List<Part> parts
) {

  public static class ContentBuilder {

    private List<Part> parts = new ArrayList<>();

    public ContentBuilder addPart(Part part){
      parts.add(part);
      return this;
    }

  }
}

