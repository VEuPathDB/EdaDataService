package org.veupathdb.service.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = HelloResponseImpl.class
)
public interface HelloResponse {
  @JsonProperty("greeting")
  String getGreeting();

  @JsonProperty("greeting")
  void setGreeting(String greeting);
}
