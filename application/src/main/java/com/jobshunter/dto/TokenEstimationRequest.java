package com.jobshunter.dto;

import com.jobshunter.database.entities.AiModelEntity;
import java.util.List;

public record TokenEstimationRequest(

    List<String> prompts,
    List<Object> tools,          // tool definitions
    Object responseSchema,        // JSON schema / response_format
    AiModelEntity aiModel,
    Integer maxOutputTokens

) {}
