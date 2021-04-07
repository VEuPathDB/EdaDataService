package org.veupathdb.service.eda.ds.constraints;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.gusdb.fgputil.ListBuilder;
import org.veupathdb.service.eda.generated.model.VariableSpec;

public class DataElementSet extends HashMap<String, List<VariableSpec>> {

  private String _entityId;

  public DataElementSet entity(String entityId) {
    _entityId = entityId;
    return this;
  }

  public DataElementSet var(String elementName, VariableSpec variableSpec) {
    put(elementName, variableSpec == null ? Collections.emptyList() : ListBuilder.asList(variableSpec));
    return this;
  }

  public DataElementSet var(String elementName, List<VariableSpec> variableSpecs) {
    put(elementName, variableSpecs == null ? Collections.emptyList() : variableSpecs);
    return this;
  }

}
