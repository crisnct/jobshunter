package com.jobshunter.service.application.cost;

import com.jobshunter.database.entities.AiModelEntity;
import com.jobshunter.dto.TokensConsumed;

public interface RequestPriceService {

  double calculatePrice(TokensConsumed request, AiModelEntity model);

}
