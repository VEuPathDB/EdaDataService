package org.veupathdb.service.eda.common.plugin.constraint;

import org.veupathdb.service.eda.generated.model.DataElementConstraintPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * interface PatternSet {
 *   [AxisName: string]: {
 *     required?: boolean;
 *     entitySameAsOrDescendedFromEntityOf?: string; //
 *     patterns: [{
 *       type?: TypeEnum[];
 *       shape?: ShapeEnum[];
 *     }];
 *   }
 * }
 * if var1 is at index x and var2 is at index x+1, then var2 must be a member of the same entity as var1, or a descendant
 */
public class ConstraintSpec extends ArrayList<DataElementConstraintPattern> {

  private List<List<String>> _dependencyOrder;

  @SafeVarargs
  public final ConstraintSpec dependencyOrder(List<String>... orderedElementNames) {
    _dependencyOrder = Arrays.asList(orderedElementNames);
    return this;
  }

  public List<List<String>> getDependencyOrder() {
    return _dependencyOrder;
  }

  public ConstraintPattern pattern() {
    ConstraintPattern pattern = new ConstraintPattern(this);
    add(pattern);
    return pattern;
  }
}
