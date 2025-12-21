package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;
import java.util.List;

public record SearchJobOrder(UserEntity user, List<EngineType> engines, int iterations) {
}
