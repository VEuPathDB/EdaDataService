package org.veupathdb.service.edads.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import org.veupathdb.service.edads.generated.model.VariableSpec;

public class EntityDef extends ArrayList<VariableDef> {

  private final String _id;

  public EntityDef(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

  public boolean hasVariable(VariableSpec var) {
    return getVariableOpt(var).isPresent();
  }

  public VariableDef getVariable(VariableSpec var) {
    return getVariableOpt(var).orElseThrow(() -> new RuntimeException("Variable " + var + " not available on entity " + _id));
  }

  private Optional<VariableDef> getVariableOpt(VariableSpec var) {
    return stream()
        .filter(v -> v.getEntityId().equals(var.getEntityId()) && v.getVariableId().equals(var.getEntityId()))
        .findFirst();
  }
}
