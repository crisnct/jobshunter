package com.jobshunter.dto.gptRequest.tools;

import lombok.Data;

@Data
public class UserLocation {
  private String type="approximate";
  private String country;
  private String city;
  private String region;
}
