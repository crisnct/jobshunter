package com.jobshunter.model;

import com.jobshunter.database.entities.UserEntity;

public record SearchJobOrder(
    UserEntity user,
    boolean searchCompanies,
    EngineSelection engineSelection
) {

}
