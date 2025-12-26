package com.jobshunter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

  @Max(100)
  private int score;

  @Size(max = 255)
  @NotNull
  private String url;

  @JsonIgnore
  private String description;

  private String source;

  private Long promptId;

  public Job(int score, String url, String source) {
    this.score = score;
    this.url = url;
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Job job = (Job) o;
    return Objects.equals(url, job.url);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(url);
  }

}