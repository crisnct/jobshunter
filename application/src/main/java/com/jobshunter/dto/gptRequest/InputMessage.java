package com.jobshunter.dto.gptRequest;

public record InputMessage(String type, String text) implements InputObj {

}