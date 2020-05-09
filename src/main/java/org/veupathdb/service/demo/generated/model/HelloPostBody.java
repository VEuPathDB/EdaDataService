package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(
    as = HelloPostBodyImpl.class
)
public interface HelloPostBody {
  @JsonProperty("greet")
  Object getGreet();

  @JsonProperty("greet")
  void setGreet(Object greet);

  @JsonProperty("description")
  Object getDescription();

  @JsonProperty("description")
  void setDescription(Object description);

  @JsonProperty("type")
  Object getType();

  @JsonProperty("type")
  void setType(Object type);
}
