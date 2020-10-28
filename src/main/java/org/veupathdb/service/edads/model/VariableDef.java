package org.veupathdb.service.edads.model;

import org.veupathdb.service.edads.generated.model.APIVariableType;

public class VariableDef {

  private final String _id;
  private final APIVariableType _type;

  public VariableDef(String id, APIVariableType type) {
    _id = id;
    _type = type;
  }

  public String getId() {
    return _id;
  }

  public APIVariableType getType() {
    return _type;
  }
}
