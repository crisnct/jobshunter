package com.jobshunter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

  @JsonIgnore
  private final Map<JobMetadataType, Object> metadata = new HashMap<>();
  @Max(100)
  private int score;
  @NotNull
  private String url;
  private String source;
  private Long promptId;

  public Job(String url) {
    this.url = url;
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

  public void addMetadata(JobMetadataType type, Object value) {
    this.metadata.put(type, value);
  }

  public <T> T getMetadata(JobMetadataType type) {
    //noinspection unchecked
    return (T) metadata.get(type);
  }

}