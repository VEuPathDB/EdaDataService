package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "status",
    "message"
})
public class UnauthorizedResponseImpl implements UnauthorizedResponse {
  @JsonProperty("status")
  private UnauthorizedResponse.StatusType status;

  @JsonProperty("message")
  private String message;

  @JsonProperty("status")
  public UnauthorizedResponse.StatusType getStatus() {
    return this.status;
  }

  @JsonProperty("status")
  public void setStatus(UnauthorizedResponse.StatusType status) {
    this.status = status;
  }

  @JsonProperty("message")
  public String getMessage() {
    return this.message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }
}
