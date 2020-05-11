package org.veupathdb.service.demo.generated.model;

import com.fasterxml.jackson.annotation.*;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("invalid-input")
@JsonPropertyOrder({
    "status",
    "message",
    "errors"
})
public class InvalidInputErrorImpl implements InvalidInputError {
  @JsonProperty("status")
  private final String status = _DISCRIMINATOR_TYPE_NAME;

  @JsonProperty("message")
  private String message;

  @JsonProperty("errors")
  private InvalidInputError.ErrorsType errors;

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

  @JsonProperty("errors")
  public InvalidInputError.ErrorsType getErrors() {
    return this.errors;
  }

  @JsonProperty("errors")
  public void setErrors(InvalidInputError.ErrorsType errors) {
    this.errors = errors;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonPropertyOrder({
      "general",
      "byKey"
  })
  public static class ErrorsTypeImpl implements InvalidInputError.ErrorsType {
    @JsonProperty("general")
    private List<String> general;

    @JsonProperty("byKey")
    private InvalidInputError.ErrorsType.ByKeyType byKey;

    @JsonProperty("general")
    public List<String> getGeneral() {
      return this.general;
    }

    @JsonProperty("general")
    public void setGeneral(List<String> general) {
      this.general = general;
    }

    @JsonProperty("byKey")
    public InvalidInputError.ErrorsType.ByKeyType getByKey() {
      return this.byKey;
    }

    @JsonProperty("byKey")
    public void setByKey(InvalidInputError.ErrorsType.ByKeyType byKey) {
      this.byKey = byKey;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder
    public static class ByKeyTypeImpl implements InvalidInputError.ErrorsType.ByKeyType {
      @JsonIgnore
      private Map<String, Object> additionalProperties = new ExcludingMap();

      @JsonAnyGetter
      public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
      }

      @JsonAnySetter
      public void setAdditionalProperties(String key, Object value) {
        this.additionalProperties.put(key, value);
      }
    }
  }
}
