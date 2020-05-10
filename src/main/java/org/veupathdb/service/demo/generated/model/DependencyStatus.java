package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

@JsonDeserialize(
    as = DependencyStatusImpl.class
)
public interface DependencyStatus {
  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);

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
