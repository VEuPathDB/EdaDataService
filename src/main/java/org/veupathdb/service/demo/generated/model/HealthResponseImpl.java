package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status",
    "dependencies"
})
public class HealthResponseImpl implements HealthResponse {
  @JsonProperty("status")
  private HealthResponse.StatusType status;

  @JsonProperty("dependencies")
  private List<DependencyStatus> dependencies;

  @JsonProperty("status")
  public HealthResponse.StatusType getStatus() {
    return this.status;
  }

  @JsonProperty("status")
  public void setStatus(HealthResponse.StatusType status) {
    this.status = status;
  }

  @JsonProperty("dependencies")
  public List<DependencyStatus> getDependencies() {
    return this.dependencies;
  }

  @JsonProperty("dependencies")
  public void setDependencies(List<DependencyStatus> dependencies) {
    this.dependencies = dependencies;
  }
}
