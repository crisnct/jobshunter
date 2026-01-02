package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;
import java.util.List;

public record SearchJobOrder(
    UserEntity user,
    boolean searchCompanies,
    List<EngineSelection> engines
) {
}
