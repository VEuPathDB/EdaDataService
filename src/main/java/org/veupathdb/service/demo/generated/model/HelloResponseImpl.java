package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("greeting")
public class HelloResponseImpl implements HelloResponse {
  @JsonProperty("greeting")
  private HelloResponse.GreetingType greeting;

  @JsonProperty("greeting")
  public HelloResponse.GreetingType getGreeting() {
    return this.greeting;
  }

  @JsonProperty("greeting")
  public void setGreeting(HelloResponse.GreetingType greeting) {
    this.greeting = greeting;
  }
}
