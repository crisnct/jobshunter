package com.jobshunter.model;

/**
 * How strictly the job URLs returned by a model are checked against the URLs the provider actually saw while searching (anti-hallucination).
 */
public enum UrlVerificationMode {

  /** Keep only URLs that appear verbatim among the URLs the provider reported. */
  STRICT,

  /** Keep URLs whose host appears among the hosts the provider reported. */
  HOST,

  /** No filtering, rejected-candidates are only logged. */
  OFF
}
