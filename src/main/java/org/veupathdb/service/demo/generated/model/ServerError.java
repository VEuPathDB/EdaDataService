package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeName("server-error")
@JsonDeserialize(
    as = ServerErrorImpl.class
)
public interface ServerError extends ErrorResponse {
  String _DISCRIMINATOR_TYPE_NAME = "server-error";

  @JsonProperty("status")
  String getStatus();

  @JsonProperty("message")
  String getMessage();

  @JsonProperty("message")
  void setMessage(String message);

  @JsonProperty("requestId")
  String getRequestId();

  @JsonProperty("requestId")
  void setRequestId(String requestId);
}
