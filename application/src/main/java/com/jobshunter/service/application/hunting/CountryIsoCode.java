package com.jobshunter.service.application.hunting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.jobshunter.dto.serpResponse.Country;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class CountryIsoCode {

  private final JsonMapper mapper;
  private Map<String, String> countryIsoCodes;

  @PostConstruct
  public void init() throws IOException {
    countryIsoCodes = new ConcurrentHashMap<>(parseGoogleCountries());
  }

  private Map<String, String> parseGoogleCountries() throws IOException {
    String location = "google-countries.json";
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location)) {
      if (inputStream == null) {
        throw new RuntimeException("Google countries file not found: " + location);
      }
      log.debug("Parsing Google countries from {}", location);
      List<Country> countries = mapper.readValue(inputStream, new TypeReference<List<Country>>() {
      });
      return countries.stream()
          .collect(Collectors.toMap(
              country -> country.countryName().toUpperCase(),
              country -> country.countryCode().toLowerCase()
          ));
    }
  }

  public String getCode(String country) {
    return countryIsoCodes.get(country.toUpperCase());
  }

  public List<String> getAllCountries() {
    return new ArrayList<>(countryIsoCodes.keySet()).stream()
        .sorted()
        .toList();
  }

}
