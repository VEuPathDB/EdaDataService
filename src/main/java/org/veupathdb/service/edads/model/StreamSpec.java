package org.veupathdb.service.edads.model;

import java.util.ArrayList;
import java.util.List;
import org.gusdb.fgputil.validation.ValidationBundle;

public class StreamSpec extends ArrayList<VariableDef> {

  private EntityDef _entity;

  public StreamSpec(EntityDef entity) {
    _entity = entity;
  }

  public EntityDef getEntity() {
    return _entity;
  }
}
