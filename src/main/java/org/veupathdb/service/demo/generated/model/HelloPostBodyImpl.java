package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "greet",
    "description",
    "type"
})
public class HelloPostBodyImpl implements HelloPostBody {
  @JsonProperty("greet")
  private Object greet;

  @JsonProperty("description")
  private Object description;

  @JsonProperty("type")
  private Object type;

  @JsonProperty("greet")
  public Object getGreet() {
    return this.greet;
  }

  @JsonProperty("greet")
  public void setGreet(Object greet) {
    this.greet = greet;
  }

  @JsonProperty("description")
  public Object getDescription() {
    return this.description;
  }

  @JsonProperty("description")
  public void setDescription(Object description) {
    this.description = description;
  }

  @JsonProperty("type")
  public Object getType() {
    return this.type;
  }

  @JsonProperty("type")
  public void setType(Object type) {
    this.type = type;
  }
}
