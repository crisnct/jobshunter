package com.jobshunter.dto.gptRequest;

import java.util.List;

public record Input(String role, List<InputObj> content) {

}