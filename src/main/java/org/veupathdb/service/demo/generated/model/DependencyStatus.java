package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = DependencyStatusImpl.class
)
public interface DependencyStatus {
  @JsonProperty("name")
  String getName();

  @JsonProperty("name")
  void setName(String name);

  @JsonProperty("reachable")
  boolean getReachable();

  @JsonProperty("reachable")
  void setReachable(boolean reachable);

  @JsonProperty("online")
  OnlineType getOnline();

  @JsonProperty("online")
  void setOnline(OnlineType online);

  enum OnlineType {
    @JsonProperty("yes")
    YES("yes"),

    @JsonProperty("unknown")
    UNKNOWN("unknown"),

    @JsonProperty("no")
    NO("no");

    private String name;

    OnlineType(String name) {
      this.name = name;
    }
  }
}
