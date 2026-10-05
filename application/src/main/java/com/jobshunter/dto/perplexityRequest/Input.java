package com.jobshunter.dto.perplexityRequest;

import java.util.List;

public record Input(String role, List<InputObj> content) {

}
