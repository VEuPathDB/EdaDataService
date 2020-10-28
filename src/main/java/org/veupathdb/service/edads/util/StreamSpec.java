package org.veupathdb.service.edads.util;

import java.util.ArrayList;

public class StreamSpec extends ArrayList<VariableDef> {

  private EntityDef _entity;

  public StreamSpec(EntityDef entity) {
    _entity = entity;
  }

  public EntityDef getEntity() {
    return _entity;
  }
}
