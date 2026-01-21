package com.jobshunter.controller;

import com.jobshunter.service.application.hunting.CountryIsoCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/misc")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MiscController {

  private final CountryIsoCode countryIsoCode;

  @GetMapping("/countries")
  public ResponseEntity<List<String>> getCountries() {
    log.info("Fetching all countries");
    List<String> countries = countryIsoCode.getAllCountries();
    return ResponseEntity.ok(countries);
  }

}