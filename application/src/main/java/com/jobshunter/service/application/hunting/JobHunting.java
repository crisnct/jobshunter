package com.jobshunter.service.application.hunting;

import com.jobshunter.model.EngineType;
import com.jobshunter.service.application.hunting.hunters.GeminiJobHunting;
import com.jobshunter.service.application.hunting.hunters.GptJobHunting;
import com.jobshunter.service.application.hunting.hunters.GrokJobHunting;
import com.jobshunter.service.application.hunting.hunters.SerpJobHunting;

/**
 * Common identity contract for all job hunting implementations.
 * <p>
 * Specific capabilities are expressed through additional interfaces
 * ({@link JobByPromptHunting}, {@link JobByCompanyHunting}).
 */
public sealed interface JobHunting
    permits SerpJobHunting, GeminiJobHunting, GptJobHunting, GrokJobHunting {

  EngineType getEngineType();

}
