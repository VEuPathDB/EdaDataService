package org.veupathdb.service.eda.ds.constraints;

import java.util.Arrays;
import java.util.Collections;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.VisualizationDataElementConstraintImpl;

public class VisualizationDataElement extends VisualizationDataElementConstraintImpl {

  private final ConstraintPattern _parent;

  public VisualizationDataElement(ConstraintPattern parent) {
    _parent = parent;
    // set defaults
    setIsRequired(true);
    setAllowedTypes(null);
    setAllowedShapes(null);
    setMinNumVars(1);
    setMaxNumVars(1);
  }

  public VisualizationDataElement required(boolean isRequired) {
    setIsRequired(isRequired);
    return this;
  }

  public VisualizationDataElement types(APIVariableType... allowedTypes) {
    setAllowedTypes(Arrays.asList(allowedTypes));
    return this;
  }

  public VisualizationDataElement shapes(APIVariableDataShape... allowedShapes) {
    setAllowedShapes(Arrays.asList(allowedShapes));
    return this;
  }

  public VisualizationDataElement min(int minNumVars) {
    setMinNumVars(minNumVars);
    return this;
  }

  public VisualizationDataElement max(int maxNumVars) {
    setMaxNumVars(maxNumVars);
    return this;
  }

  public VisualizationDataElement element(String name) {
    return _parent.element(name);
  }

  public ConstraintPattern pattern() {
    return _parent.pattern();
  }

  public ConstraintSpec done() {
    return _parent.done();
  }
}
