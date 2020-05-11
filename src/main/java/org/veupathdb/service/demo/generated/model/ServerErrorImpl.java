package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("server-error")
@JsonPropertyOrder({
    "status",
    "message",
    "requestId"
})
public class ServerErrorImpl implements ServerError {
  @JsonProperty("status")
  private final String status = _DISCRIMINATOR_TYPE_NAME;

  @JsonProperty("message")
  private String message;

  @JsonProperty("requestId")
  private String requestId;

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

  @JsonProperty("requestId")
  public String getRequestId() {
    return this.requestId;
  }

  @JsonProperty("requestId")
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }
}
