package com.jobshunter.dto;

public record InputFile(String type, String file_id) implements InputObj {

  public InputFile(String file_id) {
    this("input_file", file_id);
  }
}
