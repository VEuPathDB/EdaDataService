package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeName("bad-request")
@JsonDeserialize(
    as = BadRequestErrorImpl.class
)
public interface BadRequestError extends ErrorResponse {
  String _DISCRIMINATOR_TYPE_NAME = "bad-request";

  @JsonProperty("status")
  String getStatus();

  @JsonProperty("message")
  String getMessage();

  @JsonProperty("message")
  void setMessage(String message);
}
