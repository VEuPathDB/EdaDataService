package org.veupathdb.service.edads.util;

import java.util.ArrayList;

public class StreamSpec extends ArrayList<String> {

  private String _name;
  private String _entityId;

  public StreamSpec(String name, String entityId) {
    _name = name;
    _entityId = entityId;
  }

  public String getName() {
    return _name;
  }

  public String getEntityId() {
    return _entityId;
  }

  public StreamSpec addVariable(String variableId) {
    add(variableId);
    return this;
  }
}
