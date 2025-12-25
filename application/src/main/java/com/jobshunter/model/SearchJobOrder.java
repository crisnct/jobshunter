package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import java.util.List;

public record SearchJobOrder(
    UserEntity user,
    List<EngineSelection> engines,
    int iterations
) {
}
