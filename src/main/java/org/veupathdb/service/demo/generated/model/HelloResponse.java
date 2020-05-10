package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = HelloResponseImpl.class
)
public interface HelloResponse {
  @JsonProperty("greeting")
  GreetingType getGreeting();

  @JsonProperty("greeting")
  void setGreeting(GreetingType greeting);

  @JsonProperty("anotherType")
  AnotherType getAnotherType();

  @JsonProperty("anotherType")
  void setAnotherType(AnotherType anotherType);

  enum GreetingType {
    @JsonProperty("Hello World")
    HELLOWORLD("Hello World");

    private String name;

    GreetingType(String name) {
      this.name = name;
    }
  }
}
