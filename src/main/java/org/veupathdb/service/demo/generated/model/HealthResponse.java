package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(
    as = HealthResponseImpl.class
)
public interface HealthResponse {
  @JsonProperty("status")
  StatusType getStatus();

  @JsonProperty("status")
  void setStatus(StatusType status);

  @JsonProperty("dependencies")
  List<DependencyStatus> getDependencies();

  @JsonProperty("dependencies")
  void setDependencies(List<DependencyStatus> dependencies);

  enum StatusType {
    @JsonProperty("healthy")
    HEALTHY("healthy"),

    @JsonProperty("unhealthy")
    UNHEALTHY("unhealthy");

    private String name;

    StatusType(String name) {
      this.name = name;
    }
  }
}
