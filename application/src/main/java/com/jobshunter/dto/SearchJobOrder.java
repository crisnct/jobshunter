package com.jobshunter.dto;

import com.jobshunter.database.entities.UserEntity;

public record SearchJobOrder(UserEntity user, String gptModel, int iterations) {

}
