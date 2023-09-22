package org.veupathdb.service.eda.merging.core.derivedvars;

import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.generated.model.DerivationType;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.List;
import java.util.Map;

/**
 * Parent class providing logic and interface common to all Transform derived variables, that is: a derived variable
 * that operates on data in a single row to produce a single value in the same entity.  Instances of this
 * class represent a derived variable with a certain configuration which will be generated across an entire set of
 * rows.  Thus, the getValue() method's implementation should be stateless/pure so its results are reproducible
 * regardless of subset.
 *
 * @param <T> type of configuration object for this derived variable plugin
 */
public abstract class Transform<T> extends AbstractDerivedVariable<T> {

  /**
   * Primary data implementation method which produces a single value from a row of values.  The incoming row
   * should NOT be manipulated; it is only for pulling out values needed to calculate this derived value.
   *
   * @param row row of existing values
   * @return derived variable value for this row
   */
  public abstract String getValue(Map<String,String> row);

  @Override
  public final DerivationType getDerivationType() {
    return DerivationType.TRANSFORM;
  }

  @Override
  public final void validateDependedVariableLocations() {
    // find the ancestors of the entity this var is declared on; dependant vars must live on the same entity as this var or an ancestor
    List<String> ancestorIds = _metadata.getAncestors(getEntity()).stream().map(EntityDef::getId).toList();
    for (VariableSpec spec : getRequiredInputVars()) {
      _metadata.getVariable(spec).orElseThrow(() ->
          new BadRequestException("Input variable for transform derived var " + getFunctionName() + " does not exist."));
      if (!spec.getEntityId().equals(getEntity().getId()) && !ancestorIds.contains(spec.getEntityId())) {
        throw new BadRequestException("Transform derived vars can only use input variables on the same entity on which they are declared or on an ancestor.");
      }
    }
  }
}
