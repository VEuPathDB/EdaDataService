package org.veupathdb.service.edads.util;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.veupathdb.service.edads.generated.model.VariableSpec;

public class EntityDef extends ArrayList<VariableDef> {

  private final String _id;
  private final String _displayName;
  private final String _idColumnName;

  public EntityDef(String id, String displayName, String idColumnName) {
    _id = id;
    _displayName = displayName;
    _idColumnName = idColumnName;
  }

  public String getId() {
    return _id;
  }

  public String getIdColumnName() {
    return _idColumnName;
  }

  public boolean hasVariable(VariableSpec var) {
    return getVariableOpt(var).isPresent();
  }

  public VariableDef getVariable(VariableSpec var) {
    return getVariableOpt(var).orElseThrow(() -> new RuntimeException("Variable " + var + " not available on entity " + _id));
  }

  private Optional<VariableDef> getVariableOpt(VariableSpec var) {
    return stream()
        .filter(v -> v.getEntityId().equals(var.getEntityId()) && v.getVariableId().equals(var.getVariableId()))
        .findFirst();
  }

  @Override
  public String toString() {
    return new JSONObject()
      .put("id", _id)
      .put("displayName", _displayName)
      .put("idColumnName", _idColumnName)
      .put("variables", stream()
        .map(var -> var.getEntityId() + "." + var.getVariableId() + ":" + var.getType().toString().toLowerCase())
        .collect(Collectors.toList()))
      .toString(2);
  }

}
