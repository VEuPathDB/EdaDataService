package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = UnauthorizedResponseImpl.class
)
public interface UnauthorizedResponse {
  @JsonProperty("status")
  StatusType getStatus();

  @JsonProperty("status")
  void setStatus(StatusType status);

  @JsonProperty("message")
  String getMessage();

  @JsonProperty("message")
  void setMessage(String message);

  enum StatusType {
    @JsonProperty("unauthorized")
    UNAUTHORIZED("unauthorized");

    private String name;

    StatusType(String name) {
      this.name = name;
    }
  }
}
