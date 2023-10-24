package org.veupathdb.service.eda.common.plugin.constraint;

import org.veupathdb.service.eda.generated.model.DataElementConstraintPatternImpl;

public class ConstraintPattern extends DataElementConstraintPatternImpl {

  private final ConstraintSpec _parent;

  public ConstraintPattern(ConstraintSpec parent) {
    _parent = parent;
  }

  public VisualizationDataElement element(String name) {
    VisualizationDataElement element = new VisualizationDataElement(this);
    setAdditionalProperties(name, element);
    return element;
  }

  public ConstraintPattern pattern() {
    return _parent.pattern();
  }

  public ConstraintSpec done() {
    return _parent;
  }
}
