package com.jobshunter.dto.gptRequest;

import com.jobshunter.database.entities.UserPromptEntity;
import com.jobshunter.dto.EngineType;

public record GptJobSearchRequest(String username, UserPromptEntity prompt, String fileId, EngineType engine) {

}
