package org.veupathdb.service.eda.merge.plugins.transforms;

import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpecImpl;
import org.veupathdb.service.eda.generated.model.RelatedObservationMinTimeIntervalConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;

import java.util.List;
import java.util.Map;

/**
 * Related Observation Operations (Original Requirements)
 *
 * We implement this feature by allowing the user to create a Time to Next variable on repeated measures entities.
 *   Ror example, on the Participant Repeated Measure entity, create a variable that captures, for all
 *   observations of PCR positive, the time to the nearest observation of a high fever
 *
 * The user flow is:
 *   - choose a repeated measures entity (eg, Participant Repeated Measures)
 *   - choose a boolean variable to identify anchor entities
 *     - Remind the user that they can create a membership variable if no existing boolean var is sufficient
 *     - the ‘yes’ guys are the entities on which we will provide values for this derived variables.  the ‘no’ guys get null values.
 *   - similar for identifying a target variable
 *   - the user chooses a ‘time’ variable
 *   - optionally specifies a minimal interval to consider
 *
 * The computation:
 *   - for each anchor entity, we discover its parent entity
 *   - we scan all target RMs belonging to that parent
 *   - identify the one that has the nearest time variable value to the anchor (above the minimum interval, if specified)
 *   - assign the discovered time interval value as the derived variable value
 *
 * An advanced version of this feature (possibly in a follow up phase) is to allow anchor and target to be in different entities.  For example find the time between installation of bed nets and reduction of mosquito bites.
 *
 * How it works: we combine a number of 'helper' derived vars to produce this result:
 *
 *   A Reduction plugin that pulls set of [ boolean value + timestamp + ID ] from child entity
 *   B Transform plugin that does aggregation of A values and assigns min to each ID
 *   C Transform plugin that inherits B and assigns the correct value
 */
public class RelativeObservationMinTimeInterval extends Transform<RelatedObservationMinTimeIntervalConfig> {

  private RelatedObservationMinTimeIntervalConfig _config;
  private VariableSpec _calculatorDerivedVar;
  private String _calculatorDerivedVarColName;
  private String _idColName;

  @Override
  protected Class<RelatedObservationMinTimeIntervalConfig> getConfigClass() {
    return RelatedObservationMinTimeIntervalConfig.class;
  }

  @Override
  protected void acceptConfig(RelatedObservationMinTimeIntervalConfig config) throws ValidationException {
    _config = config;

    // this entity must also be the anchor variable's entity
    if (!_config.getAnchorVariable().getEntityId().equals(getEntityId())) {
      throw new ValidationException("Anchor variable must be a member of the relative observation derived variable's entity");
    }

    // must be able to find a shared parent entity between this entity and the target var's entity
    List<String> anchorEntityAncestorIds = getAncestorEntities(getEntityId());
    List<String> targetEntityAncestorIds = getAncestorEntities(_config.getTargetVariable().getEntityId());
    for (String anchorAncestorId: anchorEntityAncestorIds) {
      if (targetEntityAncestorIds.contains(anchorAncestorId)) {
        // found least common ancestor
        _calculatorDerivedVar = VariableDef.newVariableSpec(anchorAncestorId, getVariableId() + "_calc");
        _calculatorDerivedVarColName = VariableDef.toDotNotation(_calculatorDerivedVar);
        break;
      }
    }
    if (_calculatorDerivedVar == null) {
      throw new ValidationException("There is no ancestor entity shared by the anchor and target variables' entities.");
    }

    // pre-assign ID column name for this entity so we can look up each row's value later
    _idColName = VariableDef.toDotNotation(_metadata.getEntity(getEntityId()).orElseThrow().getIdColumnDef());
  }

  private List<String> getAncestorEntities(String entityId) {
    EntityDef anchorEntity = _metadata.getEntity(entityId).orElseThrow();
    return _metadata.getAncestors(anchorEntity).stream().map(EntityDef::getId).toList();
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    // nothing to do here
  }

  @Override
  public List<DerivedVariableSpec> getDependedDerivedVarSpecs() {
    DerivedVariableSpec dvSpec = new DerivedVariableSpecImpl();
    dvSpec.setEntityId(_calculatorDerivedVar.getEntityId());
    dvSpec.setVariableId(_calculatorDerivedVar.getVariableId());
    dvSpec.setDisplayName(_calculatorDerivedVar.getVariableId());
    dvSpec.setFunctionName(RelativeObservationCalculator.FUNCTION_NAME);
    dvSpec.setConfig(_config);
    return List.of(dvSpec);
  }

  @Override
  public String getFunctionName() {
    return "relativeObservationMinTimeInterval";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_calculatorDerivedVar);
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.INTEGER;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.CONTINUOUS;
  }

  @Override
  public String getValue(Map<String, String> row) {
    JSONObject sharedValues = new JSONObject(row.get(_calculatorDerivedVarColName));
    long value = sharedValues.optLong(row.get(_idColName), -1);
    return value == -1 ? /* no value for this row */ "" : String.valueOf(value);
  }

}
