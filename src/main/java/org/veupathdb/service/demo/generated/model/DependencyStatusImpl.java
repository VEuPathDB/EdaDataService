package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "reachable",
    "online"
})
public class DependencyStatusImpl implements DependencyStatus {
  @JsonProperty("name")
  private String name;

  @JsonProperty("reachable")
  private boolean reachable;

  @JsonProperty("online")
  private DependencyStatus.OnlineType online;

  @JsonProperty("name")
  public String getName() {
    return this.name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("reachable")
  public boolean getReachable() {
    return this.reachable;
  }

  @JsonProperty("reachable")
  public void setReachable(boolean reachable) {
    this.reachable = reachable;
  }

  @JsonProperty("online")
  public DependencyStatus.OnlineType getOnline() {
    return this.online;
  }

  @JsonProperty("online")
  public void setOnline(DependencyStatus.OnlineType online) {
    this.online = online;
  }
}
