package com.jobshunter.service.application.hunting;

import com.jobshunter.model.EngineType;

/**
 * Common identity contract for all job hunting implementations.
 * <p>
 * Sub-interfaces define the specific hunting capabilities
 * (prompt-based search, company-based search, etc.).
 */
public sealed interface JobHunting permits JobByPromptHunting, JobByCompanyHunting {

  EngineType getEngineType();

}
