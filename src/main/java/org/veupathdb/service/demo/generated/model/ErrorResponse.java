package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "status"
)
@JsonSubTypes({
    @JsonSubTypes.Type(org.veupathdb.service.demo.generated.model.InvalidInputError.class),
    @JsonSubTypes.Type(org.veupathdb.service.demo.generated.model.BadRequestError.class),
    @JsonSubTypes.Type(org.veupathdb.service.demo.generated.model.UnauthorizedError.class),
    @JsonSubTypes.Type(org.veupathdb.service.demo.generated.model.ServerError.class),
    @JsonSubTypes.Type(org.veupathdb.service.demo.generated.model.ErrorResponse.class)
})
@JsonDeserialize(
    as = ErrorResponseImpl.class
)
public interface ErrorResponse {
  String _DISCRIMINATOR_TYPE_NAME = "ErrorResponse";

  @JsonProperty("status")
  String getStatus();

  @JsonProperty("message")
  String getMessage();

  @JsonProperty("message")
  void setMessage(String message);
}
