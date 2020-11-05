package org.veupathdb.service.edads.util;

import java.util.ArrayList;

public class StreamSpec extends ArrayList<String> {

  private String _entityId;

  public StreamSpec(String entityId) {
    _entityId = entityId;
  }

  public String getEntityId() {
    return _entityId;
  }

  public StreamSpec addVariable(String variableId) {
    add(variableId);
    return this;
  }
}
