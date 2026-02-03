package com.jobshunter.dto.gptRequest.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jobshunter.StringUtils;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Tools(
    String type,
    @JsonProperty("user_location")
    UserLocation userLocation
) {

  public static class ToolsBuilder {

    private String type;

    @JsonProperty("user_location")
    private UserLocation userLocation = null;

    private FunctionDefinition function;

    public ToolsBuilder setLightweightSearch() {
      type = "web_search_preview";
      return this;
    }

    public ToolsBuilder setWebSearch() {
      type = "web_search";
      return this;
    }

    public ToolsBuilder setFunction(FunctionDefinition function) {
      this.function = function;
      return this;
    }

    public ToolsBuilder setCountry(String country) {
      userLocationCheck();
      userLocation.setCountry(country);
      return this;
    }

    public ToolsBuilder setCity(String city) {
      userLocationCheck();
      userLocation.setCity(StringUtils.removeDiacritics(city));
      return this;
    }

    private void userLocationCheck() {
      if (userLocation == null) {
        userLocation = new UserLocation();
      }
    }

  }

}