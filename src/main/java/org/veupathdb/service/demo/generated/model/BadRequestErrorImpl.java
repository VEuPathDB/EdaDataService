package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("bad-request")
@JsonPropertyOrder({
    "status",
    "message"
})
public class BadRequestErrorImpl implements BadRequestError {
  @JsonProperty("status")
  private final String status = _DISCRIMINATOR_TYPE_NAME;

  @JsonProperty("message")
  private String message;

  @JsonProperty("status")
  public String getStatus() {
    return this.status;
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
