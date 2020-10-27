package org.veupathdb.service.edads.model;

import org.veupathdb.service.edads.generated.model.APIVariableType;

public class VariableDef {

  private final String _id;
  private final String _name;
  private final APIVariableType _type;

  public VariableDef(String id, String name, APIVariableType type) {
    _id = id;
    _name = name;
    _type = type;
  }

  public String getId() {
    return _id;
  }

  public String getName() {
    return _name;
  }

  public APIVariableType getType() {
    return _type;
  }
}
