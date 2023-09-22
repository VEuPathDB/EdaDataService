package org.veupathdb.service.eda.merging.core.derivedvars;

import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.DerivationType;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Parent class providing logic and interface common to all Reduction derived variables, that is: a derived variable
 * that collects values from rows of a descendant entity and reduces them into a single value.  Instances of this
 * class represent a derived variable with a certain configuration which will be generated across an entire set of
 * rows; to do so, it provides an implementation of a Reducer, which generates a single reduction value (one row on the
 * output entity) and then is thrown away.  A new Reducer is created to produce the reduction derived variable value
 * on the next row.
 *
 * @param <T> type of configuration object for this derived variable plugin
 */
public abstract class Reduction<T> extends AbstractDerivedVariable<T> {

  /**
   * Implementations of this interface produce reduction derived variable values from a set of incoming descendant
   * entity rows.
   */
  public interface Reducer {

    /**
     * Adds a row to this reduction; values should be incorporated into a state where a final value can be returned
     * by getResultingValue()
     *
     * @param nextRow the next row to process
     */
    void addRow(Map<String,String> nextRow);

    /**
     * Produces the resulting value.  After this method is called, no more calls will be made to addRow(); however,
     * it may be called before any calls to addRow occur (if there are zero input rows).  Thus any initialization
     * needed in this class should happen in a constructor or by instantiating fields in their declarations.
     *
     * @return final value for this derived variable
     */
    String getResultingValue();
  }

  /**
   * Returns an implementation of a Reducer which will operate on a set of descendant entity input rows to create a
   * value on this entity's row.  A new Reducer is requested for each row, so it can contain state used to aggregate
   * information before returning a resulting value.
   *
   * @return instance of a Reducer which will create a reduction derived variable value for a single row
   */
  public abstract Reducer createReducer();

  private StreamSpec _inputStreamSpec;

  @Override
  public final DerivationType getDerivationType() {
    return DerivationType.REDUCTION;
  }

  public StreamSpec getInputStreamSpec() {
    // cache this so only created once
    if (_inputStreamSpec == null) {
      EntityDef entity = _metadata.getEntity(getReductionEntityId()).orElseThrow();
      List<VariableDef> idColumns = _metadata.getTabularColumns(entity, Collections.emptyList());
      _inputStreamSpec = new StreamSpec(UUID.randomUUID().toString(), entity.getId())
          .addVars(getRequiredInputVars().stream()
              // filter out IDs of the requested entity and its ancestors
              .filter(var -> idColumns.stream().noneMatch(idCol -> VariableDef.isSameVariable(idCol, var)))
              .toList())
          .setFiltersOverride(getFiltersOverride());
    }
    return _inputStreamSpec;
  }

  protected List<APIFilter> getFiltersOverride() {
    return null;
  }

  @Override
  public void validateDependedVariableLocations() {
    // the common entity of the input vars must be the same as or a descendant of the target entity
    String inputVarsEntityId = getReductionEntityId();
    List<String> ancestorIds = _metadata
        .getAncestors(_metadata.getEntity(inputVarsEntityId).orElseThrow())
        .stream().map(EntityDef::getId).toList();
    if (!inputVarsEntityId.equals(getEntityId()) && !ancestorIds.contains(getEntityId())) {
      throw new BadRequestException("Input vars configured for reduction derived var " + getFunctionName() + " are not on the target or a descendant entity.");
    }
  }

  private String getReductionEntityId() {
    // depended vars must all be in the same branch of the entity tree; confirm this and find the lowest entity (farthest from root)
    EntityDef lowestEntity = null;
    for (VariableSpec spec : getRequiredInputVars()) {

      // validate the variable
      VariableDef variable = _metadata.getVariable(spec).orElseThrow(() ->
          new BadRequestException("Input variable for reduction derived var " + getFunctionName() + " does not exist."));

      // find entity for this variable (entity should always be valid, but need EntityDef to find ancestors)
      EntityDef entity = _metadata.getEntity(variable.getEntityId()).orElseThrow();

      // if do not yet have a lowest entity or find a lower one in the same branch, assign lowest to this one
      if (lowestEntity == null || _metadata.isEntityAncestorOf(lowestEntity, entity)) {
        lowestEntity = entity;
      }
      else if (!_metadata.isEntityAncestorOf(entity, lowestEntity)) {
        // entities are not in the same branch; throw exception
        throw new BadRequestException("Not all input variables for reduction derived var " + getFunctionName() + " are in the same branch of the entity tree.");
      }
      // else entity is an ancestor of the current lowest, which is fine; keep looking
    }
    if (lowestEntity == null) {
      throw new IllegalStateException("No required input vars specified in reduction plugin " + getFunctionName());
    }
    return lowestEntity.getId();
  }
}
