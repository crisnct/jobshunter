package com.jobshunter.dto.gptRequest.tools;

import lombok.Builder;

@Builder
public record Tools(String type, UserLocation userLocation) {

  public static class ToolsBuilder {

    private String type;

    private UserLocation userLocation = null;

    private FunctionDefinition function;

    public ToolsBuilder setLightweightSearch(){
      type = "web_search_preview";
      return this;
    }

    public ToolsBuilder setDeepSearch(){
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
      userLocation.setCity(city);
      return this;
    }

    private void userLocationCheck() {
      if (userLocation == null) {
        userLocation = new UserLocation();
      }
    }

  }

}