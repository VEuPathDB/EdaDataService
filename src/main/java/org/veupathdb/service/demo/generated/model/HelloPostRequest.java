package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = HelloPostRequestImpl.class
)
public interface HelloPostRequest {
  @JsonProperty("greet")
  String getGreet();

  @JsonProperty("greet")
  void setGreet(String greet);
}
