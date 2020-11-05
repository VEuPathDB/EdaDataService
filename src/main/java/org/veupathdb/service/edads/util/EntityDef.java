package org.veupathdb.service.edads.util;

import java.util.HashMap;

public class EntityDef extends HashMap<String, VariableDef> {

  private final String _id;

  public EntityDef(String id) {
    _id = id;
  }

  public String getId() {
    return _id;
  }

}
