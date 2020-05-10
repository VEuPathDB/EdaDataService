package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "greeting",
    "anotherType"
})
public class HelloResponseImpl implements HelloResponse {
  @JsonProperty("greeting")
  private HelloResponse.GreetingType greeting;

  @JsonProperty("anotherType")
  private AnotherType anotherType;

  @JsonProperty("greeting")
  public HelloResponse.GreetingType getGreeting() {
    return this.greeting;
  }

  @JsonProperty("greeting")
  public void setGreeting(HelloResponse.GreetingType greeting) {
    this.greeting = greeting;
  }

  @JsonProperty("anotherType")
  public AnotherType getAnotherType() {
    return this.anotherType;
  }

  @JsonProperty("anotherType")
  public void setAnotherType(AnotherType anotherType) {
    this.anotherType = anotherType;
  }
}
