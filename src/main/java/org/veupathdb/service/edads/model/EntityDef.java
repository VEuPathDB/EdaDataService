package org.veupathdb.service.edads.model;

import java.util.HashMap;

public class EntityDef extends HashMap<String, VariableDef> {

  private final String _id;
  private final String _name;

  public EntityDef(String id, String name) {
    _id = id;
    _name = name;
  }

  public String getId() {
    return _id;
  }

  public String getName() {
    return _name;
  }

}
