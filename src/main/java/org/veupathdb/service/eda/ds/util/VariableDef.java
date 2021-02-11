package org.veupathdb.service.eda.ds.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.veupathdb.service.eda.generated.model.APIVariable;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.generated.model.VariableSpecImpl;

/**
 * Wrapper containing only what a plugin would need to know about a variable for
 * the purposes of validation; its name, parent entity, and data type
 */
@JsonPropertyOrder({
    "entityId",
    "variableId"
})
public class VariableDef extends VariableSpecImpl {

  @JsonProperty("type")
  private final APIVariableType _type;

  public VariableDef(String entityId, String variableId, APIVariableType type) {
    setEntityId(entityId);
    setVariableId(variableId);
    _type = type;
  }

  public VariableDef(String entityId, APIVariable var) {
    setEntityId(entityId);
    setVariableId(var.getId());
    _type = var.getType();
  }

  @JsonProperty("type")
  public APIVariableType getType() {
    return _type;
  }

  public String toString() {
    return JsonUtil.serialize(this);
  }

  public String toColumnName() {
    return toColumnName(this);
  }

  public static String toColumnName(VariableSpec spec) {
    return spec.getVariableId();
    // FIXME: change to dot notation once we are accessing stream service
    //return spec.getEntityId() + "." + spec.getVariableId();
  }
}
