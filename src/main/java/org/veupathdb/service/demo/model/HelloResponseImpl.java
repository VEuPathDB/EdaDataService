package org.veupathdb.service.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("greeting")
public class HelloResponseImpl implements HelloResponse {
  @JsonProperty("greeting")
  private String greeting;

  @JsonProperty("greeting")
  public String getGreeting() {
    return this.greeting;
  }

  @JsonProperty("greeting")
  public void setGreeting(String greeting) {
    this.greeting = greeting;
  }
}
