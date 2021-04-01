package org.veupathdb.service.eda.ds.constraints;

import org.veupathdb.service.eda.generated.model.VisualizationDataElementConstraintPatternImpl;

public class ConstraintPattern extends VisualizationDataElementConstraintPatternImpl {

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
