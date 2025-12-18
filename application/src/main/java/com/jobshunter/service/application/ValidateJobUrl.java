package com.jobshunter.service.application;

@FunctionalInterface
public interface ValidateJobUrl {
  boolean isValidJob(String jobURL);
}
