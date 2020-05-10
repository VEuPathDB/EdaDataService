package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("greet")
public class HelloPostRequestImpl implements HelloPostRequest {
  @JsonProperty("greet")
  private String greet;

  @JsonProperty("greet")
  public String getGreet() {
    return this.greet;
  }

  @JsonProperty("greet")
  public void setGreet(String greet) {
    this.greet = greet;
  }
}
